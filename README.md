# 如何使用shiro做权限控制

> shiro不适合与jwt整合，它的最佳实现应该是将session存入redis中

## 1 简单案例

shiro官网有一个简单案例，按照这个案例可以很容易理解shiro的用法，地址是`http://shiro.apache.org/tutorial.html`

### 1.1 包

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.1.4.RELEASE</version>
</parent>


<dependencies>
    <dependency>
        <groupId>org.apache.shiro</groupId>
        <artifactId>shiro-core</artifactId>
        <version>1.4.1</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>

```

### 1.2 shiro配置文件shiro.ini

这个是官网copy过来的

```ini
# =============================================================================
# Tutorial INI configuration
#
# Usernames/passwords are based on the classic Mel Brooks' film "Spaceballs" :)
# =============================================================================

# -----------------------------------------------------------------------------
# Users and their (optional) assigned roles
# 定义一个账号和账号拥有的角色
# username = password, role1, role2, ..., roleN
# 用户名=密码，角色1，角色2，...，角色n
# -----------------------------------------------------------------------------
[users]
root = secret, admin
guest = guest, guest
presidentskroob = 12345, president
darkhelmet = ludicrousspeed, darklord, schwartz
lonestarr = vespa, goodguy, schwartz

# -----------------------------------------------------------------------------
# Roles with assigned permissions
# 给角色分配的权限
# roleName = perm1, perm2, ..., permN
# 角色名=权限1，权限2，...，权限n
# -----------------------------------------------------------------------------
[roles]
admin = *
schwartz = lightsaber:*
goodguy = winnebago:drive:eagle5

```

### 1.3 创建一个测试类进行测试

```java
@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class ShiroApplicationTest {

    @Test
    public void myFirstShiro() {
        log.info("My First Apache Shiro Application");

        //1.使用Shiro的IniSecurityManagerFactory实现来提取shiro.ini位于类路径根目录的文件
        Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.ini");

        //2.该方法分析INI文件并返回一个SecurityManager的实例
        SecurityManager securityManager = factory.getInstance();

        //3.设置SecurityManager为静态（内存）单例，
        // 但是更复杂的应用程序环境通常会将SecurityManager放置在特定于应用程序的内存中
        //（例如，放置在Web应用程序ServletContext或Spring，Guice或JBoss DI容器实例中）。
        SecurityUtils.setSecurityManager(securityManager);

        //获取当前用户
        Subject currentUser = SecurityUtils.getSubject();

        //判断用户是否已认证
        if (!currentUser.isAuthenticated()) {
            //collect user principals and credentials in a gui specific manner
            //such as username/password html form, X509 certificate, OpenID, etc.
            //We'll use the username/password example here since it is the most common.
            //创建用户认证对象
            UsernamePasswordToken token = new UsernamePasswordToken("lonestarr", "vespa");

            //this is all you have to do to support 'remember me' (no config - built in!):
            token.setRememberMe(true);

            try {
                //认证
                currentUser.login(token);
                //if no exception, that's it, we're done!

                //print their identifying principal (in this case, a username):
                log.info( "User [" + currentUser.getPrincipal() + "] logged in successfully." );

                //用户是否拥有某个角色
                if ( currentUser.hasRole( "schwartz" ) ) {
                    log.info("May the Schwartz be with you!" );
                } else {
                    log.info( "Hello, mere mortal." );
                }

                //用户是否拥有某个权限
                if ( currentUser.isPermitted( "lightsaber:wield" ) ) {
                    log.info("You may use a lightsaber ring.  Use it wisely.");
                } else {
                    log.info("Sorry, lightsaber rings are for schwartz masters only.");
                }

            } catch (UnknownAccountException uae) {
                log.error("username wasn't in the system, show them an error message");
                //username wasn't in the system, show them an error message?
            } catch (IncorrectCredentialsException ice) {
                //password didn't match, try again?
                log.error("password didn't match, try again?");

            } catch (LockedAccountException lae) {

                log.error("account for that username is locked - can't login.  Show them a message?");
                //account for that username is locked - can't login.  Show them a message?
            } catch (AuthenticationException ae) {

                log.error("unexpected condition - error?");
                //unexpected condition - error?
            }
        }

        System.exit(0);
    }
}

```

看控制台打印的日志，认证，授权成功

## 2 源码解析

### 2.1 认证过程

```java
//创建用户认证对象
UsernamePasswordToken token = new UsernamePasswordToken("lonestarr", "vespa");
//认证
currentUser.login(token);
```

下面我们就来看一下具体是如何进行认证的

使用debug一步一步走

进入`login`方法

```java
public void login(AuthenticationToken token) throws AuthenticationException {
    //忽略
    clearRunAsIdentitiesInternal();
    //又一个login方法，很有可能是认证的
    Subject subject = securityManager.login(this, token);

    PrincipalCollection principals;

    String host = null;

    if (subject instanceof DelegatingSubject) {
        DelegatingSubject delegating = (DelegatingSubject) subject;
        //we have to do this in case there are assumed identities - we don't want to lose the 'real' principals:
        principals = delegating.principals;
        host = delegating.host;
    } else {
        principals = subject.getPrincipals();
    }

    if (principals == null || principals.isEmpty()) {
        String msg = "Principals returned from securityManager.login( token ) returned a null or " +
            "empty value.  This value must be non null and populated with one or more elements.";
        throw new IllegalStateException(msg);
    }
    this.principals = principals;
    this.authenticated = true;
    if (token instanceof HostAuthenticationToken) {
        host = ((HostAuthenticationToken) token).getHost();
    }
    if (host != null) {
        this.host = host;
    }
    Session session = subject.getSession(false);
    if (session != null) {
        this.session = decorate(session);
    } else {
        this.session = null;
    }
}
```

进入`login`方法

```java
/**
     * First authenticates the {@code AuthenticationToken} argument, and if successful, constructs a
     * {@code Subject} instance representing the authenticated account's identity.
     * <p/>
     * Once constructed, the {@code Subject} instance is then {@link #bind bound} to the application for
     * subsequent access before being returned to the caller.
     *
     * @param token the authenticationToken to process for the login attempt.
     * @return a Subject representing the authenticated user.
     * @throws AuthenticationException if there is a problem authenticating the specified {@code token}.
     */
public Subject login(Subject subject, AuthenticationToken token) throws AuthenticationException {
    AuthenticationInfo info;
    try {
        //认证方法
        info = authenticate(token);
    } catch (AuthenticationException ae) {
        try {
            //认证失败后的操作
            onFailedLogin(token, ae, subject);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("onFailedLogin method threw an " +
                         "exception.  Logging and propagating original AuthenticationException.", e);
            }
        }
        throw ae; //propagate
    }

    //创建一个用户认证成功的对象，并保存在上下文中，方便用户获取
    Subject loggedIn = createSubject(token, info, subject);

     //认证成功后的操作
    onSuccessfulLogin(token, info, loggedIn);

    return loggedIn;
}

```

进入`authenticate`认证方法

```java
/**
     * Delegates to the wrapped {@link org.apache.shiro.authc.Authenticator Authenticator} for authentication.
     */
public AuthenticationInfo authenticate(AuthenticationToken token) throws AuthenticationException {
    //debug发现this.authenticator中对象的实际类型是ModularRealmAuthenticator
    return this.authenticator.authenticate(token);
}
```

进入`authenticate`认证方法

```java
/**
     * Implementation of the {@link Authenticator} interface that functions in the following manner:
     * <ol>
     * <li>Calls template {@link #doAuthenticate doAuthenticate} method for subclass execution of the actual
     * authentication behavior.</li>
     * <li>If an {@code AuthenticationException} is thrown during {@code doAuthenticate},
     * {@link #notifyFailure(AuthenticationToken, AuthenticationException) notify} any registered
     * {@link AuthenticationListener AuthenticationListener}s of the exception and then propagate the exception
     * for the caller to handle.</li>
     * <li>If no exception is thrown (indicating a successful login),
     * {@link #notifySuccess(AuthenticationToken, AuthenticationInfo) notify} any registered
     * {@link AuthenticationListener AuthenticationListener}s of the successful attempt.</li>
     * <li>Return the {@code AuthenticationInfo}</li>
     * </ol>
     *
     * @param token the submitted token representing the subject's (user's) login principals and credentials.
     * @return the AuthenticationInfo referencing the authenticated user's account data.
     * @throws AuthenticationException if there is any problem during the authentication process - see the
     *                                 interface's JavaDoc for a more detailed explanation.
     */
public final AuthenticationInfo authenticate(AuthenticationToken token) throws AuthenticationException {

    if (token == null) {
        throw new IllegalArgumentException("Method argument (authentication token) cannot be null.");
    }

    log.trace("Authentication attempt received for token [{}]", token);

    AuthenticationInfo info;
    try {
        //认证方法，返回认证成功的对象
        info = doAuthenticate(token);
        if (info == null) {
            String msg = "No account information found for authentication token [" + token + "] by this " +
                "Authenticator instance.  Please check that it is configured correctly.";
            throw new AuthenticationException(msg);
        }
    } catch (Throwable t) {
        AuthenticationException ae = null;
        if (t instanceof AuthenticationException) {
            ae = (AuthenticationException) t;
        }
        if (ae == null) {
            //Exception thrown was not an expected AuthenticationException.  Therefore it is probably a little more
            //severe or unexpected.  So, wrap in an AuthenticationException, log to warn, and propagate:
            String msg = "Authentication failed for token submission [" + token + "].  Possible unexpected " +
                "error? (Typical or expected login exceptions should extend from AuthenticationException).";
            ae = new AuthenticationException(msg, t);
            if (log.isWarnEnabled())
                log.warn(msg, t);
        }
        try {
            notifyFailure(token, ae);
        } catch (Throwable t2) {
            if (log.isWarnEnabled()) {
                String msg = "Unable to send notification for failed authentication attempt - listener error?.  " +
                    "Please check your AuthenticationListener implementation(s).  Logging sending exception " +
                    "and propagating original AuthenticationException instead...";
                log.warn(msg, t2);
            }
        }


        throw ae;
    }

    log.debug("Authentication successful for token [{}].  Returned account [{}]", token, info);

    notifySuccess(token, info);

    return info;
}

```

进入`doAuthenticate`认证方法

```java
/**
     * Attempts to authenticate the given token by iterating over the internal collection of
     * {@link Realm}s.  For each realm, first the {@link Realm#supports(org.apache.shiro.authc.AuthenticationToken)}
     * method will be called to determine if the realm supports the {@code authenticationToken} method argument.
     * <p/>
     * If a realm does support
     * the token, its {@link Realm#getAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken)}
     * method will be called.  If the realm returns a non-null account, the token will be
     * considered authenticated for that realm and the account data recorded.  If the realm returns {@code null},
     * the next realm will be consulted.  If no realms support the token or all supporting realms return null,
     * an {@link AuthenticationException} will be thrown to indicate that the user could not be authenticated.
     * <p/>
     * After all realms have been consulted, the information from each realm is aggregated into a single
     * {@link AuthenticationInfo} object and returned.
     *
     * @param authenticationToken the token containing the authentication principal and credentials for the
     *                            user being authenticated.
     * @return account information attributed to the authenticated user.
     * @throws IllegalStateException   if no realms have been configured at the time this method is invoked
     * @throws AuthenticationException if the user could not be authenticated or the user is denied authentication
     *                                 for the given principal and credentials.
     */
protected AuthenticationInfo doAuthenticate(AuthenticationToken authenticationToken) throws AuthenticationException {
    //确保realm验证不为空
    assertRealmsConfigured();
    //得到realm列表，realm是具体做授权认证的
    Collection<Realm> realms = getRealms();
    if (realms.size() == 1) {
        //授权认证方式只有一个的时候，默认是IniRealm，即读取ini配置文件授权认证
        return doSingleRealmAuthentication(realms.iterator().next(), authenticationToken);
    } else {
        //授权认证方式有多个的时候，会使用多种方式，只要有一个成功即可
        return doMultiRealmAuthentication(realms, authenticationToken);
    }
}
```

#### 2.1.1 授权认证方式只有一个的时候

进入`doSingleRealmAuthentication`方法

```java
/**
     * Performs the authentication attempt by interacting with the single configured realm, which is significantly
     * simpler than performing multi-realm logic.
     *
     * @param realm the realm to consult for AuthenticationInfo.
     * @param token the submitted AuthenticationToken representing the subject's (user's) log-in principals and credentials.
     * @return the AuthenticationInfo associated with the user account corresponding to the specified {@code token}
     */
protected AuthenticationInfo doSingleRealmAuthentication(Realm realm, AuthenticationToken token) {
    //判断是否支持该种认证方式
    if (!realm.supports(token)) {
        String msg = "Realm [" + realm + "] does not support authentication token [" +
            token + "].  Please ensure that the appropriate Realm implementation is " +
            "configured correctly or that the realm accepts AuthenticationTokens of this type.";
        throw new UnsupportedTokenException(msg);
    }
    //调用realm的认证方法进行授权认证，并返回认证成功的对象
    AuthenticationInfo info = realm.getAuthenticationInfo(token);
    if (info == null) {
        String msg = "Realm [" + realm + "] was unable to find account data for the " +
            "submitted AuthenticationToken [" + token + "].";
        throw new UnknownAccountException(msg);
    }
    return info;
}
```

进入`getAuthenticationInfo`方法

```java
public final AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

    //从缓存中获取用户认证对象
    AuthenticationInfo info = getCachedAuthenticationInfo(token);
    if (info == null) {
        //otherwise not cached, perform the lookup:
        //缓存中没有就要调用认证方法，进行认证了
        //所以为什么重写这个方法，明白了吧
        info = doGetAuthenticationInfo(token);
        log.debug("Looked up AuthenticationInfo [{}] from doGetAuthenticationInfo", info);
        if (token != null && info != null) {
            //向缓存中添加认证成功的用户对象
            cacheAuthenticationInfoIfPossible(token, info);
        }
    } else {
        log.debug("Using cached authentication info [{}] to perform credentials matching.", info);
    }

    if (info != null) {
        //密码校验，两个密码是否相等
        assertCredentialsMatch(token, info);
    } else {
        log.debug("No AuthenticationInfo found for submitted AuthenticationToken [{}].  Returning null.", token);
    }

    return info;
}
```



#### 2.1.2 授权认证方式有多个的时候

进入`doMultiRealmAuthentication`方法

```java
/**
     * Performs the multi-realm authentication attempt by calling back to a {@link AuthenticationStrategy} object
     * as each realm is consulted for {@code AuthenticationInfo} for the specified {@code token}.
     *
     * @param realms the multiple realms configured on this Authenticator instance.
     * @param token  the submitted AuthenticationToken representing the subject's (user's) log-in principals and credentials.
     * @return an aggregated AuthenticationInfo instance representing account data across all the successfully
     *         consulted realms.
     */
protected AuthenticationInfo doMultiRealmAuthentication(Collection<Realm> realms, AuthenticationToken token) {

    AuthenticationStrategy strategy = getAuthenticationStrategy();

    //创建一个空的用户信息认证对象
    AuthenticationInfo aggregate = strategy.beforeAllAttempts(realms, token);

    if (log.isTraceEnabled()) {
        log.trace("Iterating through {} realms for PAM authentication", realms.size());
    }

    //遍历认证方式
    for (Realm realm : realms) {

        aggregate = strategy.beforeAttempt(realm, token, aggregate);

        if (realm.supports(token)) {

            log.trace("Attempting to authenticate token [{}] using realm [{}]", token, realm);

            AuthenticationInfo info = null;
            Throwable t = null;
            try {
                //认证
                info = realm.getAuthenticationInfo(token);
            } catch (Throwable throwable) {
                t = throwable;
                if (log.isDebugEnabled()) {
                    String msg = "Realm [" + realm + "] threw an exception during a multi-realm authentication attempt:";
                    log.debug(msg, t);
                }
            }

            //获取不为空的认证对象
            aggregate = strategy.afterAttempt(realm, token, info, aggregate, t);

        } else {
            log.debug("Realm [{}] does not support token {}.  Skipping realm.", realm, token);
        }
    }

    aggregate = strategy.afterAllAttempts(token, aggregate);

    return aggregate;
}

```

### 2.2 认证具体代码

我们发现，不管是哪种认证方式，都是调用的realm的`getAuthenticationInfo`方法

```java
/**
     * This implementation functions as follows:
     * <ol>
     * <li>It attempts to acquire any cached {@link AuthenticationInfo} corresponding to the specified
     * {@link AuthenticationToken} argument.  If a cached value is found, it will be used for credentials matching,
     * alleviating the need to perform any lookups with a data source.</li>
     * <li>If there is no cached {@link AuthenticationInfo} found, delegate to the
     * {@link #doGetAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken)} method to perform the actual
     * lookup.  If authentication caching is enabled and possible, any returned info object will be
     * {@link #cacheAuthenticationInfoIfPossible(org.apache.shiro.authc.AuthenticationToken, org.apache.shiro.authc.AuthenticationInfo) cached}
     * to be used in future authentication attempts.</li>
     * <li>If an AuthenticationInfo instance is not found in the cache or by lookup, {@code null} is returned to
     * indicate an account cannot be found.</li>
     * <li>If an AuthenticationInfo instance is found (either cached or via lookup), ensure the submitted
     * AuthenticationToken's credentials match the expected {@code AuthenticationInfo}'s credentials using the
     * {@link #getCredentialsMatcher() credentialsMatcher}.  This means that credentials are always verified
     * for an authentication attempt.</li>
     * </ol>
     *
     * @param token the submitted account principal and credentials.
     * @return the AuthenticationInfo corresponding to the given {@code token}, or {@code null} if no
     *         AuthenticationInfo could be found.
     * @throws AuthenticationException if authentication failed.
     */
    public final AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

        //获取缓存区的认证对象
        AuthenticationInfo info = getCachedAuthenticationInfo(token);
        if (info == null) {
            //otherwise not cached, perform the lookup:
            //这里测试的使用ini配置文件认证的，所以doGetAuthenticationInfo也必须是inireaalm中的
            //也可以使用jdbcRealm方式，连接数据库库查询
            //这个是系统（内存或数据库）中存储的用户信息构建的认证对象
            info = doGetAuthenticationInfo(token);
            log.debug("Looked up AuthenticationInfo [{}] from doGetAuthenticationInfo", info);
            if (token != null && info != null) {
                cacheAuthenticationInfoIfPossible(token, info);
            }
        } else {
            log.debug("Using cached authentication info [{}] to perform credentials matching.", info);
        }

        if (info != null) {
            //判断用户提交的和系统存储的是否一致，真正验证密码是否匹配的方法
            assertCredentialsMatch(token, info);
        } else {
            log.debug("No AuthenticationInfo found for submitted AuthenticationToken [{}].  Returning null.", token);
        }

        return info;
    }
```

进入真正验证密码是否匹配的方法`assertCredentialsMatch`

```java
/**
     * Asserts that the submitted {@code AuthenticationToken}'s credentials match the stored account
     * {@code AuthenticationInfo}'s credentials, and if not, throws an {@link AuthenticationException}.
     *
     * @param token the submitted authentication token 用户提交的
     * @param info  the AuthenticationInfo corresponding to the given {@code token} 系统存储的
     * @throws AuthenticationException if the token's credentials do not match the stored account credentials.
     * 两个对象信息进行比对
     */
protected void assertCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) throws AuthenticationException {
    CredentialsMatcher cm = getCredentialsMatcher();
    if (cm != null) {
        //具体如何校验，我们不管，有兴趣的可以看一下
        if (!cm.doCredentialsMatch(token, info)) {
            //not successful - throw an exception to indicate this:
            String msg = "Submitted credentials for token [" + token + "] did not match the expected credentials.";
            throw new IncorrectCredentialsException(msg);
        }
    } else {
        throw new AuthenticationException("A CredentialsMatcher must be configured in order to verify " +
                                          "credentials during authentication.  If you do not wish for credentials to be examined, you " +
                                          "can configure an " + AllowAllCredentialsMatcher.class.getName() + " instance.");
    }
}
```

下面是两种系统提供的认证方式

#### 2.2.1 IniRealm

进入`IniRealm`类中的`doGetAuthenticationInfo`方法

```java
protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    UsernamePasswordToken upToken = (UsernamePasswordToken) token;
    //根据用户名从内存中获取用户对象
    SimpleAccount account = getUser(upToken.getUsername());

    if (account != null) {

        if (account.isLocked()) {
            throw new LockedAccountException("Account [" + account + "] is locked.");
        }
        if (account.isCredentialsExpired()) {
            String msg = "The credentials for account [" + account + "] are expired";
            throw new ExpiredCredentialsException(msg);
        }

    }

    return account;
}
```

进入`getUser`方法

```java
protected SimpleAccount getUser(String username) {
    USERS_LOCK.readLock().lock();
    try {
        //protected final Map<String, SimpleAccount> users; //username-to-SimpleAccount
        //map集合，程序启动会将ini配置文件中的用户数据加载到这个users中
        return this.users.get(username);
    } finally {
        USERS_LOCK.readLock().unlock();
    }
}
```

#### 2.2.2 JdbcRealm

这种认证方式，系统已经编写好了sql语句，如果想使用，那么表的字段就必须按照规定命名，这对我们使用造成了不便。

接下来我们进入`JdbcRealm`类中的`doGetAuthenticationInfo`方法

```java
/*--------------------------------------------
    |               M E T H O D S               |
    ============================================*/

protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

    //强转为真实类型
    UsernamePasswordToken upToken = (UsernamePasswordToken) token;
    String username = upToken.getUsername();

    // Null username is invalid
    if (username == null) {
        throw new AccountException("Null usernames are not allowed by this realm.");
    }

    Connection conn = null;
    SimpleAuthenticationInfo info = null;
    try {
        //数据库连接，使用原始的jdbc方式
        conn = dataSource.getConnection();

        String password = null;
        String salt = null;
        switch (saltStyle) {
            case NO_SALT:
                //根据用户名查询数据库获取密码
                password = getPasswordForUser(conn, username)[0];
                break;
            case CRYPT:
                // TODO: separate password and hash from getPasswordForUser[0]
                throw new ConfigurationException("Not implemented yet");
                //break;
            case COLUMN:
                String[] queryResults = getPasswordForUser(conn, username);
                password = queryResults[0];
                salt = queryResults[1];
                break;
            case EXTERNAL:
                password = getPasswordForUser(conn, username)[0];
                salt = getSaltForUser(username);
        }

        if (password == null) {
            throw new UnknownAccountException("No account found for user [" + username + "]");
        }

        //根据用户名和密码创建一个用户信息认证对象
        info = new SimpleAuthenticationInfo(username, password.toCharArray(), getName());

        if (salt != null) {
            //设置盐
            info.setCredentialsSalt(ByteSource.Util.bytes(salt));
        }

    } catch (SQLException e) {
        final String message = "There was a SQL error while authenticating user [" + username + "]";
        if (log.isErrorEnabled()) {
            log.error(message, e);
        }

        // Rethrow any SQL errors as an authentication exception
        throw new AuthenticationException(message, e);
    } finally {
        JdbcUtils.closeConnection(conn);
    }

    return info;
}

/**
 * 根据用户名获取数据库中的密码
 */
private String[] getPasswordForUser(Connection conn, String username) throws SQLException {

    String[] result;
    boolean returningSeparatedSalt = false;
    switch (saltStyle) {
        case NO_SALT:
        case CRYPT:
        case EXTERNAL:
            result = new String[1];
            break;
        default:
            result = new String[2];
            returningSeparatedSalt = true;
    }

    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
        ps = conn.prepareStatement(authenticationQuery);
        ps.setString(1, username);

        // Execute query
        rs = ps.executeQuery();

        // Loop over results - although we are only expecting one result, since usernames should be unique
        boolean foundResult = false;
        while (rs.next()) {

            // Check to ensure only one row is processed
            if (foundResult) {
                throw new AuthenticationException("More than one user row found for user [" + username + "]. Usernames must be unique.");
            }

            result[0] = rs.getString(1);
            if (returningSeparatedSalt) {
                result[1] = rs.getString(2);
            }

            foundResult = true;
        }
    } finally {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(ps);
    }

    return result;
}
```

所以如果我们要实现自己的数据库连接认证，就重写`JdbcRealm`的`doGetAuthenticationInfo`方法

到此，认证就说完了

### 2.3 授权过程

我们就看一下`currentUser.hasRole( "schwartz" )`这个过程是如何实现的

```java
public boolean hasRole(String roleIdentifier) {
    //前面是判断用户是否已经认证
    //后面是具体判断用户是否拥有roleIdentifier角色
    return hasPrincipals() && securityManager.hasRole(getPrincipals(), roleIdentifier);
}
```

进入`hasRole`方法

```java
public boolean hasRole(PrincipalCollection principals, String roleIdentifier) {
    //又封装了一次，debug模式发现authorizer的真实类型是ModularRealmAuthorizer
    return this.authorizer.hasRole(principals, roleIdentifier);
}
```

```java
/**
     * Returns <code>true</code> if any of the configured realms'
     * {@link #hasRole(org.apache.shiro.subject.PrincipalCollection, String)} call returns <code>true</code>,
     * <code>false</code> otherwise.
     */
    public boolean hasRole(PrincipalCollection principals, String roleIdentifier) {
        //确保有一种授权认证方式可以使用
        assertRealmsConfigured();
        //遍历授权认证方式
        for (Realm realm : getRealms()) {、
            //这里模拟realm的真实类型是IniRealm
            if (!(realm instanceof Authorizer)) continue;
             //IniRealm实现了多个接口，所以可以使用该接口类型调用接口对应方法
            if (((Authorizer) realm).hasRole(principals, roleIdentifier)) {
                return true;
            }
        }
        return false;
    }

```

进入`hasRole`方法

```java
public boolean hasRole(PrincipalCollection principal, String roleIdentifier) {
    //根据用户认证对象获取用户授权对象
    AuthorizationInfo info = getAuthorizationInfo(principal);
    return hasRole(roleIdentifier, info);
}
```

进入`getAuthorizationInfo`方法

```java
/**
     * Returns an account's authorization-specific information for the specified {@code principals},
     * or {@code null} if no account could be found.  The resulting {@code AuthorizationInfo} object is used
     * by the other method implementations in this class to automatically perform access control checks for the
     * corresponding {@code Subject}.
     * <p/>
     * This implementation obtains the actual {@code AuthorizationInfo} object from the subclass's
     * implementation of
     * {@link #doGetAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection) doGetAuthorizationInfo}, and then
     * caches it for efficient reuse if caching is enabled (see below).
     * <p/>
     * Invocations of this method should be thought of as completely orthogonal to acquiring
     * {@link #getAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken) authenticationInfo}, since either could
     * occur in any order.
     * <p/>
     * For example, in &quot;Remember Me&quot; scenarios, the user identity is remembered (and
     * assumed) for their current session and an authentication attempt during that session might never occur.
     * But because their identity would be remembered, that is sufficient enough information to call this method to
     * execute any necessary authorization checks.  For this reason, authentication and authorization should be
     * loosely coupled and not depend on each other.
     * <h3>Caching</h3>
     * The {@code AuthorizationInfo} values returned from this method are cached for efficient reuse
     * if caching is enabled.  Caching is enabled automatically when an {@link #setAuthorizationCache authorizationCache}
     * instance has been explicitly configured, or if a {@link #setCacheManager cacheManager} has been configured, which
     * will be used to lazily create the {@code authorizationCache} as needed.
     * <p/>
     * If caching is enabled, the authorization cache will be checked first and if found, will return the cached
     * {@code AuthorizationInfo} immediately.  If caching is disabled, or there is a cache miss, the authorization
     * info will be looked up from the underlying data store via the
     * {@link #doGetAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)} method, which must be implemented
     * by subclasses.
     * <h4>Changed Data</h4>
     * If caching is enabled and if any authorization data for an account is changed at
     * runtime, such as adding or removing roles and/or permissions, the subclass implementation should clear the
     * cached AuthorizationInfo for that account via the
     * {@link #clearCachedAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection) clearCachedAuthorizationInfo}
     * method.  This ensures that the next call to {@code getAuthorizationInfo(PrincipalCollection)} will
     * acquire the account's fresh authorization data, where it will then be cached for efficient reuse.  This
     * ensures that stale authorization data will not be reused.
     *
     * @param principals the corresponding Subject's identifying principals with which to look up the Subject's
     *                   {@code AuthorizationInfo}.
     * @return the authorization information for the account associated with the specified {@code principals},
     *         or {@code null} if no account could be found.
     */
protected AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals) {

    if (principals == null) {
        return null;
    }

    AuthorizationInfo info = null;

    if (log.isTraceEnabled()) {
        log.trace("Retrieving AuthorizationInfo for principals [" + principals + "]");
    }

    //创建缓存，以后就不用每次都从配置文件或数据库中加载授权对象了
    Cache<Object, AuthorizationInfo> cache = getAvailableAuthorizationCache();
    if (cache != null) {
        if (log.isTraceEnabled()) {
            log.trace("Attempting to retrieve the AuthorizationInfo from cache.");
        }
        Object key = getAuthorizationCacheKey(principals);
        //获取缓存中的用户授权对象
        info = cache.get(key);
        if (log.isTraceEnabled()) {
            if (info == null) {
                log.trace("No AuthorizationInfo found in cache for principals [" + principals + "]");
            } else {
                log.trace("AuthorizationInfo found in cache for principals [" + principals + "]");
            }
        }
    }


    if (info == null) {
        // Call template method if the info was not found in a cache
        //第一次，从配置文件或数据库中加载
        //具体获取授权信息的方法，这里也是以两种方式（内存IniRealm，数据库JdbcRealm）举例
        info = doGetAuthorizationInfo(principals);
        // If the info is not null and the cache has been created, then cache the authorization info.
        if (info != null && cache != null) {
            if (log.isTraceEnabled()) {
                log.trace("Caching authorization info for principals: [" + principals + "].");
            }
            Object key = getAuthorizationCacheKey(principals);
            cache.put(key, info);
        }
    }

    return info;
}

```

进入`doGetAuthorizationInfo`方法

#### 2.3.1 IniRealm

```java
protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    String username = getUsername(principals);
    USERS_LOCK.readLock().lock();
    try {
         //根据用户名从内存中获取用户授权对象
        return this.users.get(username);
    } finally {
        USERS_LOCK.readLock().unlock();
    }
}
```

#### 2.3.2 JdbcRealm

```java
/**
     * This implementation of the interface expects the principals collection to return a String username keyed off of
     * this realm's {@link #getName() name}
     *
     * @see #getAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)
     */
@Override
protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

    //null usernames are invalid
    if (principals == null) {
        throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
    }

    String username = (String) getAvailablePrincipal(principals);

    Connection conn = null;
    Set<String> roleNames = null;
    Set<String> permissions = null;
    try {
        conn = dataSource.getConnection();

        //使用原生的jdbc获取用户名对应的用户的角色和权限
        // Retrieve roles and permissions from database
        roleNames = getRoleNamesForUser(conn, username);
        if (permissionsLookupEnabled) {
            permissions = getPermissions(conn, username, roleNames);
        }

    } catch (SQLException e) {
        final String message = "There was a SQL error while authorizing user [" + username + "]";
        if (log.isErrorEnabled()) {
            log.error(message, e);
        }

        // Rethrow any SQL errors as an authorization exception
        throw new AuthorizationException(message, e);
    } finally {
        JdbcUtils.closeConnection(conn);
    }

    //构建用户授权对象
    SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
    //设置权限
    info.setStringPermissions(permissions);
    return info;

}

```

如果需要自定义获取角色和权限，就直接重写该方法，然后按照这个方法的格式返回一个授权对象就行了

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201107161457898.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMzMDEyOTgx,size_16,color_FFFFFF,t_70#pic_center)


上面是shiro中realm域之间的关系，我们一般实现自己的realm域都是继承`AuthorizingRealm`

## 3 shiro整合spring

shiro官网有详细教程`https://shiro.apache.org/spring.html`

### 3.1 shiro体系结构

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201107161525386.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMzMDEyOTgx,size_16,color_FFFFFF,t_70#pic_center)


shiro体系，主要就是7个部分组成，非常方便扩展，下面这个配置文件除了密码加密没有涉及到，另外6个都使用到了。

### 3.2 spring配置文件

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
       http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd
       http://www.springframework.org/schema/tx
       http://www.springframework.org/schema/tx/spring-tx.xsd
       http://www.springframework.org/schema/aop
       http://www.springframework.org/schema/aop/spring-aop.xsd
       http://www.springframework.org/schema/mvc
       http://www.springframework.org/schema/mvc/spring-mvc.xsd">

    <context:component-scan base-package="cn.lx.*"></context:component-scan>

    <bean id="placeholderConfigurer" class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
        <property name="location" value="classpath:db.properties"></property>
    </bean>

    <bean id="basicDataSource" class="org.apache.commons.dbcp.BasicDataSource">
        <property name="driverClassName" value="${driver}"></property>
        <property name="url" value="${url}"></property>
        <property name="username" value="${username}"></property>
        <property name="password" value="${password}"></property>
    </bean>

    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="dataSource" ref="basicDataSource"></property>
        <property name="configLocation" value="classpath:mybatis-config.xml"></property>
        <property name="mapperLocations" value="classpath*:mapper/*.xml"></property>
    </bean>

    <bean id="mapperScannerConfigurer" class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <property name="basePackage" value="cn.lx.shiro.dao"></property>
    </bean>
    <bean id="tx" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="basicDataSource"></property>
    </bean>
    <tx:annotation-driven proxy-target-class="true" transaction-manager="tx"></tx:annotation-driven>



    <!--                  以下是shiro的相关配置                  -->
    <bean id="shiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
        <property name="securityManager" ref="securityManager"/>
        <!-- override these for application-specific URLs if you like:
        <property name="loginUrl" value="/login.jsp"/>
        <property name="successUrl" value="/home.jsp"/>
        <property name="unauthorizedUrl" value="/unauthorized.jsp"/> -->
        <!-- The 'filters' property is not necessary since any declared javax.servlet.Filter bean  -->
        <!-- defined will be automatically acquired and available via its beanName in chain        -->
        <!-- definitions, but you can perform instance overrides or name aliases here if you like: -->
        <!-- <property name="filters">
            <util:map>
                <entry key="anAlias" value-ref="someFilter"/>
            </util:map>
        </property> -->
        <!--这个地方硬编码，不合理，应该从数据库加载-->
        <!--<property name="filterChainDefinitions">
            <value>
                # some example chain definitions:
                /admin/** = authc, roles[admin]
                /docs/** = authc, perms[document:read]
                /test/** = anon
                /login.html = anon
                /** = authc
                # more URL-to-FilterChain definitions here
            </value>
        </property>-->
        <property name="filterChainDefinitionMap" ref="filterChainDefinitionMap"/>
    </bean>
    <!--配置一个bean，这个bean实际上是一个map，通过实例工厂方法的方式获取这个map-->
    <bean id="filterChainDefinitionMap"
          factory-bean="filterChainDefinitionMapBuilder"
          factory-method="buildFilterChainDefinitionMap"/>
    <bean id="filterChainDefinitionMapBuilder" class="cn.lx.shiro.config.FilterChainDefinitionMapBuilder"/>

    <!-- Define any javax.servlet.Filter beans you want anywhere in this application context.   -->
    <!-- They will automatically be acquired by the 'shiroFilter' bean above and made available -->
    <!-- to the 'filterChainDefinitions' property.  Or you can manually/explicitly add them     -->
    <!-- to the shiroFilter's 'filters' Map if desired. See its JavaDoc for more details.       -->
    <!--<bean id="someFilter" class="..."/>
    <bean id="anotherFilter" class="..."> ... </bean>-->

    <!-- Define the realm you want to use to connect to your back-end security datasource: -->
  <!--  <bean id="myRealm" class="cn.lx.shiro.realm.MyRealm"></bean>-->

    <bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
        <!-- Single realm app.  If you have multiple realms, use the 'realms' property instead. -->
        <property name="realm" ref="myRealm"/>
        <!--缓存-->
        <property name="cacheManager" ref="memoryConstrainedCacheManager"/>
        <!--会话管理-->
        <property name="sessionManager" ref="sessionManager"/>

    </bean>
    
    <bean id="myRealm" class="cn.lx.shiro.realm.MyRealm">
        <property name="IUserService" ref="userServiceImpl"></property>
        <!--realm也有缓存，把缓存管理器给它，会自动调用，进行缓存-->
        <property name="cacheManager" ref="memoryConstrainedCacheManager"></property>
        <!--开启认证信息缓存-->
        <property name="authenticationCachingEnabled" value="true"></property>
        <!--开启授权信息缓存-->
        <property name="authorizationCachingEnabled" value="true"></property>
    </bean>

    <!--缓存
    没有设置缓存方式，则每次都需要访问数据库
    这里是保存在内存中
    我们也可以将缓存保存在数据库中
    继承EnterpriseCacheSessionDAO，重写缓存存储方式
    -->
    <bean id="memoryConstrainedCacheManager" class="org.apache.shiro.cache.MemoryConstrainedCacheManager"/>

    <!--
    会话管理，默认的会话类型是this.sessionManager = new DefaultSessionManager();
    不符合web项目需求，我们可以使用自带的DefaultWebSessionManager（传统项目，一个服务器），
    也可以继承DefaultWebSessionManager，重写会话方式，改为支持jwt的
    -->
    <bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
        <!--设置使用内存管理session缓存-->
        <property name="sessionDAO" ref="memorySessionDAO"></property>
    </bean>
	<!--创建一个基于内存的session管理器-->
    <bean id="memorySessionDAO" class="org.apache.shiro.session.mgt.eis.MemorySessionDAO"/>
   
    
    <!--生命周期-->
    <bean id="lifecycleBeanPostProcessor" class="org.apache.shiro.spring.LifecycleBeanPostProcessor"/>

    <!-- For simplest integration, so that all SecurityUtils.* methods work in all cases, -->
    <!-- make the securityManager bean a static singleton.  DO NOT do this in web         -->
    <!-- applications - see the 'Web Applications' section below instead.                 -->
    <!--<bean class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
        <property name="staticMethod" value="org.apache.shiro.SecurityUtils.setSecurityManager"/>
        <property name="arguments" ref="securityManager"/>
    </bean>-->

    <!-- Enable Shiro Annotations for Spring-configured beans.  Only run after -->
    <!-- the lifecycleBeanProcessor has run: -->
    <bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator" depends-on="lifecycleBeanPostProcessor"/>
    <bean class="org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor">
        <property name="securityManager" ref="securityManager"/>
    </bean>

</beans>
```

这是主要配置，其他配置和普通的ssm差不多

### 3.3 实例工厂方法的方式创建map集合的bean

```java
public class FilterChainDefinitionMapBuilder {

    /**
     * 这个方法产生一个map集合
     * map集合的数据是从数据库中查询出来的
     * 这里为了方便，我们就硬编码了
     *
     * @return
     */
    public LinkedHashMap<String, String> buildFilterChainDefinitionMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        /*
        /admin/** = authc, roles[admin]
         /docs/** = authc, perms[document:read]
         /test/** = anon
         /login.html = anon
         /** = authc
         */
        map.put("/admin/**", "authc, roles[admin]");
        map.put("/docs/**", "authc, perms[document:read]");
        map.put("/test/**", "anon");
        map.put("/login.html", "anon");
        map.put("/**", "authc");
        return map;
    }
}

```

### 3.4 自定义realm域

仿写JdbcRealm

```java
@Slf4j
public class MyRealm extends AuthorizingRealm {

    private IUserService iUserService;


    /**
     * Retrieves the AuthorizationInfo for the given principals from the underlying data store.  When returning
     * an instance from this method, you might want to consider using an instance of
     * {@link SimpleAuthorizationInfo SimpleAuthorizationInfo}, as it is suitable in most cases.
     *
     * @param principals the primary identifying principals of the AuthorizationInfo that should be retrieved.
     * @return the AuthorizationInfo associated with this principals.
     * @see SimpleAuthorizationInfo
     * 授权，不知道如何实现，你就仿写{@link JdbcRealm}中的该方法
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        //null usernames are invalid
        if (principals == null) {
            throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
        }

        String username = (String) getAvailablePrincipal(principals);

        Set<String> roleNames = null;
        Set<String> permissions = null;
        try {

            // Retrieve roles and permissions from database
            //根据用户名获取用户的角色名集合
            roleNames =iUserService.getRoleNamesForUser(username);

            //根据用户名获取用户的权限名集合
            permissions = iUserService.getPermissions(username);


        } catch (Exception e) {
            final String message = "There was a error while authorizing user [" + username + "]";
            if (log.isErrorEnabled()) {
                log.error(message, e);
            }

            // Rethrow any errors as an authorization exception
            throw new AuthorizationException(message, e);
        }
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
        info.setStringPermissions(permissions);
        return info;
    }

    /**
     * Retrieves authentication data from an implementation-specific datasource (RDBMS, LDAP, etc) for the given
     * authentication token.
     * <p/>
     * For most datasources, this means just 'pulling' authentication data for an associated subject/user and nothing
     * more and letting Shiro do the rest.  But in some systems, this method could actually perform EIS specific
     * log-in logic in addition to just retrieving data - it is up to the Realm implementation.
     * <p/>
     * A {@code null} return value means that no account could be associated with the specified token.
     *
     * @param token the authentication token containing the user's principal and credentials.
     * @return an {@link AuthenticationInfo} object containing account data resulting from the
     * authentication ONLY if the lookup is successful (i.e. account exists and is valid, etc.)
     * @throws AuthenticationException if there is an error acquiring data or performing
     *                                 realm-specific authentication logic for the specified <tt>token</tt>
     * 认证，不知道如何实现，你就仿写{@link JdbcRealm}中的该方法
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();

        // Null username is invalid
        if (username == null) {
            throw new AccountException("Null usernames are not allowed by this realm.");
        }

        SimpleAuthenticationInfo info = null;
        try {

            String password = null;
            String salt = null;

            password = iUserService.findPasswordByMobileOrUsername(username);

            if (password == null) {
                throw new UnknownAccountException("No account found for user [" + username + "]");
            }
            info = new SimpleAuthenticationInfo(username, password.toCharArray(), getName());


        } catch (Exception e) {
            final String message = "There was a error while authenticating user [" + username + "]";
            if (log.isErrorEnabled()) {
                log.error(message, e);
            }

            // Rethrow any errors as an authentication exception
            throw new AuthenticationException(message, e);
        }
        return info;
    }
}

```

### 3.5 登录

```java
@PostMapping(value = "/login")
public String login(@RequestBody User user) {

    Subject currentUser = SecurityUtils.getSubject();

    if (!currentUser.isAuthenticated()) {
        //collect user principals and credentials in a gui specific manner
        //such as username/password html form, X509 certificate, OpenID, etc.
        //We'll use the username/password example here since it is the most common.
        //(do you know what movie this is from? ;)
        UsernamePasswordToken token = new UsernamePasswordToken(user.getUsername(), user.getPassword());
        //this is all you have to do to support 'remember me' (no config - built in!):
        token.setRememberMe(false);
        try {
            currentUser.login(token);

            //print their identifying principal (in this case, a username):
            log.info( "User [" + currentUser.getPrincipal() + "] logged in successfully." );

            //if no exception, that's it, we're done!

        } catch (UnknownAccountException uae) {
            //username wasn't in the system, show them an error message?
        } catch (IncorrectCredentialsException ice) {
            //password didn't match, try again?
        } catch (LockedAccountException lae) {
            //account for that username is locked - can't login.  Show them a message?
        } catch (AuthenticationException ae) {
            //unexpected condition - error?
        }
    }

    return "登录成功";
}
```

上面是关键代码，其余的是通用代码，简单的增删改查，这里不贴出来了

### 3.6 测试

```java
@GetMapping
@RequiresRoles("admin")
public String test() {
    return "测试成功";
}
```

数据库中设置两个账号，其中一个拥有的`admin`角色，另一个没有，分别登录之后，访问该url，查看能否测试成功。

## 4 使用redis做shiro的缓存

shiro自带的缓存管理器是将缓存保存在内存中，这种方法在实际开发中并不可取，我们一般会使用redis管理缓存。并且传统项目中，session是保存在当前服务器，那么，如果我们的项目访问量比较大，需要增加服务器的数量（就是使用多台服务器，每台服务器上跑的都是当前完整的项目，然后借助nginx进行负载均衡），这时候，你就会发现，用户发送的请求被均匀到几个服务器中，所以用户仅仅在一个服务器登录是没有用的，必须在每一个服务器上登录一次，就算这样，用户在每个上面都登录一次，那么如果用户在其中一个服务器修改了用户信息，当前服务器session中的用户信息会被修改，别的服务器上的session信息保持原样，所以我们一般将缓存统一放到redis中，所有服务器都去redis中获取缓存，这样就解决了上面的两个问题。

### 4.1 spring整合redis

我们使用`spring-data-redis`来操作redis

官方教程`https://docs.spring.io/spring-data/redis/docs/current/reference/html/#redis:setup`

#### 4.1.1 包

```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-redis</artifactId>
    <version>2.3.5.RELEASE</version>
</dependency>

<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>3.3.0</version>
</dependency>
```

#### 4.1.2 配置RedisTemplate

```xml
<!--redis-->
<bean id="jedisConnFactory"
      class="org.springframework.data.redis.connection.jedis.JedisConnectionFactory"
      p:use-pool="true">
    <property name="hostName" value="192.168.43.33"></property>
    <property name="database" value="10"></property>
</bean>
!-- redis template definition -->
<bean id="redisTemplate"
      class="org.springframework.data.redis.core.RedisTemplate"
      p:connection-factory-ref="jedisConnFactory">
    <!--设置key为string类型，这样可以去掉乱码问题-->
    <property name="keySerializer" ref="stringRedisSerializer"></property>
</bean>
<bean id="stringRedisSerializer"
      class="org.springframework.data.redis.serializer.StringRedisSerializer">
</bean>

```

这样就可以在bean标签中注入了。

### 4.2 重写redis缓存管理器

#### 4.2.1 原始的缓存管理器

```java
public class MemoryConstrainedCacheManager extends AbstractCacheManager {
    public MemoryConstrainedCacheManager() {
    }

    protected Cache createCache(String name) {
        //创建了一个map集合，也就是说缓存就是一个map
        return new MapCache(name, new SoftHashMap());
    }
}
```

看他的父类

```java
public abstract class AbstractCacheManager implements CacheManager, Destroyable {
    //这是一个缓存队列，你就把他看成索引，它方便我们查找到具体用户的缓存
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap();

    public AbstractCacheManager() {
    }

    /**
	 * 这个就是获取缓存的方法了
	 */
    public <K, V> Cache<K, V> getCache(String name) throws IllegalArgumentException, CacheException {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Cache name cannot be null or empty.");
        } else {
            Cache cache = (Cache)this.caches.get(name);
            if (cache == null) {
                //缓存不存在，创建缓存
                cache = this.createCache(name);
                Cache existing = (Cache)this.caches.putIfAbsent(name, cache);
                if (existing != null) {
                    cache = existing;
                }
            }

            return cache;
        }
    }

    protected abstract Cache createCache(String var1) throws CacheException;

    /**
	 * 消毁缓存
	 */
    public void destroy() throws Exception {
        while(!this.caches.isEmpty()) {
            Iterator var1 = this.caches.values().iterator();

            while(var1.hasNext()) {
                Cache cache = (Cache)var1.next();
                //这个是删除用户的缓存，缓存必须实现Destroyable接口，实现destroy方法
                //它会调用destroy方法清空用户数据
                LifecycleUtils.destroy(cache);
            }

            this.caches.clear();
        }

    }

    public String toString() {
        Collection<Cache> values = this.caches.values();
        StringBuilder sb = (new StringBuilder(this.getClass().getSimpleName())).append(" with ").append(this.caches.size()).append(" cache(s)): [");
        int i = 0;

        for(Iterator var4 = values.iterator(); var4.hasNext(); ++i) {
            Cache cache = (Cache)var4.next();
            if (i > 0) {
                sb.append(", ");
            }

            sb.append(cache.toString());
        }

        sb.append("]");
        return sb.toString();
    }
}

```

#### 4.2.2 原始的用户缓存

```java
public class MapCache<K, V> implements Cache<K, V> {
    private final Map<K, V> map;
    private final String name;

    public MapCache(String name, Map<K, V> backingMap) {
        if (name == null) {
            throw new IllegalArgumentException("Cache name cannot be null.");
        } else if (backingMap == null) {
            throw new IllegalArgumentException("Backing map cannot be null.");
        } else {
            this.name = name;
            this.map = backingMap;
        }
    }

    public V get(K key) throws CacheException {
        return this.map.get(key);
    }

    public V put(K key, V value) throws CacheException {
        return this.map.put(key, value);
    }

    public V remove(K key) throws CacheException {
        return this.map.remove(key);
    }

    public void clear() throws CacheException {
        this.map.clear();
    }

    public int size() {
        return this.map.size();
    }

    public Set<K> keys() {
        Set<K> keys = this.map.keySet();
        return !keys.isEmpty() ? Collections.unmodifiableSet(keys) : Collections.emptySet();
    }

    public Collection<V> values() {
        Collection<V> values = this.map.values();
        return (Collection)(!values.isEmpty() ? Collections.unmodifiableCollection(values) : Collections.emptyList());
    }

    public String toString() {
        return "MapCache '" + this.name + "' (" + this.map.size() + " entries)";
    }
}
```

代码逻辑很简单，我们可以使用redis完成上述功能

#### 4.2.3 redis的缓存管理器

```java
@Setter
public class RedisCacheManager extends AbstractCacheManager {

    /**
     * 仿写{@link MemoryConstrainedCacheManager}
     * 创建一个redis缓存，然后缓存实现增删改查
     *
     * @param name
     * @return
     * @throws CacheException
     */
    @Override
    protected Cache createCache(String name) throws CacheException {
        return new RedisCache(name);
    }

}
```

#### 4.2.4 redis的用户缓存

```java
@Slf4j
public class RedisCache<K, V> implements Cache<K, V> , Destroyable {

    private final String name;

    public RedisCache(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Cache name cannot be null.");
        } else {
            this.name = name;
        }
    }


    @Override
    public V get(K k) throws CacheException {
        RedisTemplate<K, V> redisTemplate = ApplicationContextUtils.getBean(RedisTemplate.class);
        log.info("RedisCache:get");
        return (V) redisTemplate.opsForValue().get(name+":"+k);
    }

    @Override
    public V put(K k, V v) throws CacheException {
        RedisTemplate<String, V> redisTemplate = ApplicationContextUtils.getBean(RedisTemplate.class);
        redisTemplate.opsForValue().set(name+":"+k,v,10,TimeUnit.MINUTES);
        return null;
    }

    @Override
    public V remove(K k) throws CacheException {
        RedisTemplate redisTemplate = ApplicationContextUtils.getBean(RedisTemplate.class);
        redisTemplate.delete(name+":"+k);
        return null;
    }

    @Override
    public void clear() throws CacheException {
        RedisTemplate redisTemplate = ApplicationContextUtils.getBean(RedisTemplate.class);        
        redisTemplate.delete( keys());
    }

    @Override
    public int size() {
        return keys() .size();
    }

    @Override
    public Set<K> keys() {
        RedisTemplate redisTemplate = ApplicationContextUtils.getBean(RedisTemplate.class);
        //这种方式比直接使用keys方法效率好
        Set<K> keys = (Set<K>) redisTemplate.execute(new RedisCallback<Set<K>>() {
            /**
             * Gets called by {@link RedisTemplate} with an active Redis connection. Does not need to care about activating or
             * closing the connection or handling exceptions.
             *
             * @param connection active Redis connection
             * @return a result object or {@code null} if none
             * @throws DataAccessException
             */
            @Override
            public Set<K> doInRedis(RedisConnection connection) throws DataAccessException {
                Set<K> keys = new HashSet<K>();
               //可以找到对应名字的所有key
                Cursor<byte[]> scan = connection.scan(new ScanOptions.ScanOptionsBuilder().match(name + "*").build());
                while (scan.hasNext()) {
                    String key = new String(scan.next(), Charset.defaultCharset());
                    keys.add((K)key);
                }
                return keys;
            }
        });
        return !keys.isEmpty() ? Collections.unmodifiableSet(keys) : (Set<K>) Collections.emptySet();
    }

    @Override
    public Collection<V> values() {
        RedisTemplate redisTemplate = ApplicationContextUtils.getBean(RedisTemplate.class);
        List values = redisTemplate.opsForValue().multiGet(keys());
        return (Collection)(!values.isEmpty() ? Collections.unmodifiableCollection(values) : Collections.emptyList());

    }

    /**
     * 清空redis
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        clear();
    }
}

```

### 4.3 重写sessionDao（session存储方式）

原始的session是存储在内存，现在我们要将session存入redis中

```java
@Getter
@Setter
public class RedisCacheSessionDAO extends EnterpriseCacheSessionDAO {

    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 根据session创建sessionId
     *
     * @param session
     * @return
     */
    @Override
    protected Serializable doCreate(Session session) {
        //父类方法，生成sessionId
        Serializable sessionId = super.doCreate(session);
        //将session保存到redis中
        storeSession(sessionId, session);
        return sessionId;
    }

    /**
     * 将session存储在redis中
     *
     * @param sessionId
     * @param session
     */
    protected void storeSession(Serializable sessionId, Session session) {
        if (sessionId == null) {
            throw new NullPointerException("id argument cannot be null.");
        }
        //todo:将session存储在redis中
        redisTemplate.opsForValue().set("session:"+sessionId,session,10, TimeUnit.MINUTES);

    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        Session session = (Session) redisTemplate.opsForValue().get("session:"+sessionId);
        return session;
    }

    @Override
    protected void doUpdate(Session session) {
        //将session保存到redis中
        storeSession(session.getId(), session);
       }

    @Override
    protected void doDelete(Session session) {
        redisTemplate.delete("session:"+session.getId());
    }
}
```

### 4.4 测试

#### 4.4.1 登出

```java
@PostMapping(value = "/logout")
public String logout() {

    Subject currentUser = SecurityUtils.getSubject();

    if (currentUser.isAuthenticated()) {
        currentUser.logout();
    }
    return "退出成功";
}
```

#### 4.4.2 测试

先登录，查看redis

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201107161629314.png#pic_center)


登出，查看redis

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201107161654787.png#pic_center)


就只剩下session了



## 5 shiro整合springboot

不知道为什么，我在shiro官网找不到进入spring-boot相关教程的入口，但是直接输入网址就行。。。

网址：`https://shiro.apache.org/spring-boot.html`

### 5.1 依赖

目前最新官方推荐版本

```xml
<!--shiro整合spring-boot-web-->
<dependency>
    <groupId>org.apache.shiro</groupId>
    <artifactId>shiro-spring-boot-web-starter</artifactId>
    <version>1.6.0</version>
</dependency>

<!--shiro整合spring-boot-->
<dependency>
    <groupId>org.apache.shiro</groupId>
    <artifactId>shiro-spring-boot-starter</artifactId>
    <version>1.6.0</version>
</dependency>

```

我们对比一下，两个包的区别

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201107161745285.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMzMDEyOTgx,size_16,color_FFFFFF,t_70#pic_center)


上面两个包在shiro方面没啥区别

### 5.2 shiro配置类

springboot有默认配置，也就是上面xml中定义的bean在springboot中已经帮我们定义好了

```java
@Configuration
public class ShiroConfig {


    /**
     * 注入自己的realm域
     */
    @Autowired
    private MyRealm myRealm;

    /**
     * 定义一个安全管理器
     * 在springboot项目中，它已经自动向spring容器中注入了一个，但是，
     * 他的默认设置不符合我们的需求，我们需要自己创建一个，使用自己的配置
     * 原始配置我们会在下面给出来
     * @return
     */
    @Bean
    public SessionsSecurityManager securityManager(){
        DefaultWebSecurityManager defaultWebSecurityManager=new DefaultWebSecurityManager();
        defaultWebSecurityManager.setRealm(myRealm);
        return defaultWebSecurityManager;
    }


    /**
     * 路径映射到给定的过滤器，以允许不同的路径具有不同的访问级别
     * 这个我们也需要覆盖springboot的自动配置
     * 这个很简单，我们覆盖ShiroWebAutoConfiguration定义的bean
     * @return
     */
    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();
        //匿名访问
        chainDefinition.addPathDefinition("/test/**", "anon"); 
        //登出的url
        chainDefinition.addPathDefinition("/logout", "logout");
        //其他所有路径全部需要认证
        chainDefinition.addPathDefinition("/**", "authc");
        return chainDefinition;
    }
    
     /**
     *  @DependsOn("lifecycleBeanPostProcessor") 控制bean初始化顺序
     *  表示该bean依赖于lifecycleBeanPostProcessor这个bean
     *  lifecycleBeanPostProcessor 这个spring-boot已经为我们自动注入了
     *  就在ShiroBeanAutoConfiguration中
     *  
     *  这个bean和下面那个都是参照shiro官网中的spring配置文件来创建的bean
     *  {
     *  <!-- Enable Shiro Annotations for Spring-configured beans.  Only run after -->
     *  <!-- the lifecycleBeanProcessor has run: -->
     *  <bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator" depends-on="lifecycleBeanPostProcessor"/>
     *      <bean class="org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor">
     *      <property name="securityManager" ref="securityManager"/>
     *  </bean>
     *  }
     *  官网上已经指明了如果想使用注解，就必须创建这两个bean
     * @return
     */
    @Bean
    @DependsOn("lifecycleBeanPostProcessor")
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator(){
        DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator =
                new DefaultAdvisorAutoProxyCreator();
        //shiro官网未指明需要该项配置，但在springboot中，必须加入
        // ，否则配置的匿名访问不生效
        defaultAdvisorAutoProxyCreator.setUsePrefix(true);
        return defaultAdvisorAutoProxyCreator;
    }
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(){
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor =
                new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager());
        return authorizationAttributeSourceAdvisor;
    }

}
```

接下来我们看一下springboot的自动配置是如何实现的，在idea中连续两下`shift`键，就会弹出一个类搜索的弹框，我们直接搜索`shiroconfig`，如下

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201107161815135.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMzMDEyOTgx,size_16,color_FFFFFF,t_70#pic_center)


我们找到了4个有关shiro的配置类，其中名字中带有`auto`的就知道这是springboot的自动配置类

我们首先看第一个`ShiroAutoConfiguration`的父类`AbstractShiroConfiguration`，使用idea快捷键`ctrl`+`H`就可以看到该类的继承体系

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201107161832250.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMzMDEyOTgx,size_16,color_FFFFFF,t_70#pic_center)


我们比较一下两个红框的类，我们会发现，它们会创建相同的名字的bean，但是你要注意，在`ShiroWebAutoConfiguration`类上有一个注解`@AutoConfigureBefore(ShiroAutoConfiguration.class)`，有了这个注解，就会先创建`ShiroAutoConfiguration`类中的bean，然后`ShiroWebAutoConfiguration`类中的bean覆盖它。

我们就直接看`ShiroWebAutoConfiguration`类了，首先找`securityManager`的bean

```java
@Bean
@ConditionalOnMissingBean
@Override
protected SessionsSecurityManager securityManager(List<Realm> realms) {
    //调用父类的方法
    return super.securityManager(realms);
}

/****************父类*************************/
protected SessionsSecurityManager securityManager(List<Realm> realms) {
    SessionsSecurityManager securityManager = createSecurityManager();
    //这些set其实没有必要，我们可以去看一下SecurityManager的继承体系，你就明白了
    //这些set都是一些默认set，在我们new一个DefaultSecurityManager的时候，就已经初始化了
    //例如：authenticator(),这个是认证器，它在ShiroWebAutoConfiguration类中定义了一个bean
    //，放入了容器所以我们如果要实现自己的认证器，我们就先将其放入spring中，然后再设置
    //到securityManager中
    //但是如果我们和默认设置不一样，我们就必须在这里重新设置一下
    securityManager.setAuthenticator(authenticator());
    securityManager.setAuthorizer(authorizer());
    //这里的setRealm会同步到认证器ModularRealmAuthenticator中
    securityManager.setRealms(realms);
    securityManager.setSessionManager(sessionManager());
    securityManager.setEventBus(eventBus);

    if (cacheManager != null) {
        securityManager.setCacheManager(cacheManager);
    }

    return securityManager;
}


protected SessionsSecurityManager createSecurityManager() {
    //这个创建的不是web的安全管理器，不满足我们的需要，需要修改
    DefaultSecurityManager securityManager = new DefaultSecurityManager();
    securityManager.setSubjectDAO(subjectDAO());
    securityManager.setSubjectFactory(subjectFactory());

    RememberMeManager rememberMeManager = rememberMeManager();
    if (rememberMeManager != null) {
        securityManager.setRememberMeManager(rememberMeManager);
    }

    return securityManager;
}
```

我们再看一下`SecurityManager`的继承体系

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201107161848773.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMzMDEyOTgx,size_16,color_FFFFFF,t_70#pic_center)


这就是装饰者模式

我们可以随便点开一个，就以`AuthenticatingSecurityManager`为例

```java
public abstract class AuthenticatingSecurityManager extends RealmSecurityManager {

    /**
     * The internal <code>Authenticator</code> delegate instance that this SecurityManager instance will use
     * to perform all authentication operations.
     * 认证器
     */
    private Authenticator authenticator;

    /**
     * Default no-arg constructor that initializes its internal
     * <code>authenticator</code> instance to a
     * {@link org.apache.shiro.authc.pam.ModularRealmAuthenticator ModularRealmAuthenticator}.
     */
    public AuthenticatingSecurityManager() {
        super();
        //认证器的类型是ModularRealmAuthenticator
        this.authenticator = new ModularRealmAuthenticator();
    }
}
```

我们再看看springboot注入的认证器的类型

```java
@Bean
@ConditionalOnMissingBean
@Override
protected Authenticator authenticator() {
    return super.authenticator();
}


/***********************父类 **********************/
protected Authorizer authorizer() {
    //认证器的类型是ModularRealmAuthenticator
    ModularRealmAuthorizer authorizer = new ModularRealmAuthorizer();

    if (permissionResolver != null) {
        authorizer.setPermissionResolver(permissionResolver);
    }

    if (rolePermissionResolver != null) {
        authorizer.setRolePermissionResolver(rolePermissionResolver);
    }

    return authorizer;
}
```

认证器的类型是一样的

### 5.3 整合redis参考ssm



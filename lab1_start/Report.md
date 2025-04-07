# Report

## Part 0

- `make all`

- `java ShoppingCart [unsafe | safe]`

## Part 1

1. The shared resources are `wallet.txt` and `pocket.txt`. These files are shared between the main thread executing the shopping cart logic.
2. The root problem is a TOCTOU race condition in the purchase progress. Checking and updating the balance are performed separately.

3. The operations are not atomic, so we can attack the program by introducing a delay between the two operations. To achieve the delay, we use breakpoints to delay the IDE's balance check.
4. When debugging a program in an IDE while simultaneously running a Java program in the terminal, use breakpoint to ensure that the instances reach the balance check and read the same balance.
   The terminal inputs the command to purchase a car, both instances will initially pass the balance check since they read the original balance. The IDE's execution resumes afterward, it will still proceed with adding the product to the pocket based on the outdated balance.
   As a result, the original 30,000 balance ends up purchasing two cars.

![image-20250403212440848](/Users/huyushu/Library/Application Support/typora-user-images/image-20250403212440848.png)

![image-20250403212555781](/Users/huyushu/Library/Application Support/typora-user-images/image-20250403212555781.png)

## Part 2

1. `pocket.addProduct` is also vulnerable to race conditions. When multiple concurrent requests attempt to add products to the pocket file, simultaneous writes can lead to data inconsistencies. Each operation on the shared file is performed independently without any locking or coordination, allowing interleaved execution that may corrupt the final state of the file.
2. These protections are sufficient because they address the sections where race conditions occur  by making critical operations atomic. By applying synchronization only to specific operations that manipulate shared resources, we minimize performance overhead.
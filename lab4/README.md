# JavaScript Sandboxing (optional lab 4)

### Build and run

Run the following commands to build and start the webserver on
`localhost:8080`. When deploying, the host and port will be different.

```sh
docker build -t lbs:ctf ctf
docker run --detach -p 127.0.0.1:8080:8080 lbs:ctf
```
### Stop and remove the container

To stop the server, run:

```sh
docker container stop $(docker ps --filter ancestor=lbs:ctf -q)
```

To remove the ***stopped*** container, run:

```sh
docker rm $(docker ps -a --filter ancestor=lbs:ctf -q)
```

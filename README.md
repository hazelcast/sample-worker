# Sample worker


Starts a worker and an HTTP server that delivers memory usage measured every few milliseconds.
The HTTP response consists of one timestamp followed by the measurement per line.
The server delivers the data accumulated since the last request and then forgets it.

## To build

`mvn clean package`

## To run

`java -jar worker-1.0-SNAPSHOT.jar <port>`

## Sample output

`curl localhost:8009`

```
1542277639312 52549624
1542277639323 53899672
1542277639336 53899672
1542277639347 53899672
1542277639359 53899672
1542277639371 53899672
1542277639384 53899672
1542277639397 53899672
1542277639408 53899672
1542277639420 53899672
1542277639432 53899672
```

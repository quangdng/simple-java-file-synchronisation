## File Synchronisation

The purpose of file synchronisation is to detect changes made on a source file and synchronise those changes on a 
destination file. The end result is that the destination file will "mirror" the source file.

File synchronisation needs to consider things such as efficient methods of transfering the changes to the destination
file, which is typically a remote file, and how to handle various special cases such as external changes to the 
destination file.

## Features
- A basic client/server system: the client is the source and the server is the destination.
- Files could be started in any state, whether they are synchronised or not.
- Allow client to specify who is the sender and who will be the receiver prior to starting synchronisation.
  + The server starts and waits for the client to connect.
  + The client starts and the "direction" (e.g. push or pull) is given on the command line.
  + The server receives a message from the client indicating the direction.
  + The system continues as normal.
- Allow the client to specify the BlockSize prior to starting synchronisation.
  + The server starts and waits for the client to connect.
  + The client starts and the BlockSize is given on the command line.
  + The server receives a message from the client indicating the BlockSize.
  
## Technical Aspects
- Use TCP. This will ensure reliable, in-order message delivery and also allow different block sizes to be tried 
without having to worry about instruction sizes. Use JSON for messages.

## How To Use
- Server Side: java -jar syncserver.jar -file filename
- Client Side: java -jar syncclient.jar -file filename -host hostname -direction pull -blocksize 2048

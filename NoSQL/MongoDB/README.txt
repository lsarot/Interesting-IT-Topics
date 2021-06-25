PODEMOS IR A 
LA DOC OFICIAL
https://docs.mongodb.com/manual/tutorial/install-mongodb-on-os-x/
LAS QUICK START GUIDES
https://www.mongodb.com/blog/channel/quickstart
COMPARING ATLAS TO AWS DocumentDB and AZURE CosmosDB
	//AWS tiene DocumentDB y DynamoDB, Dynamo sería el equivalente a Aurora pero para otro tipo de NoSQL.
	//DocumentDB también es administrada, pero es basada en documentos tipo MongoDB.
	//Azure cambió nombre a su CosmosDB por DocumentDB.
https://www.mongodb.com/cloud/atlas/compare


MongoDB se puede instalar en macOS con:
.brew (recommended)
.tar.gz (requiere más configuración)

* You can run MongoDB as a macOS service using brew, or you can run MongoDB manually as a background process. It is recommended to run MongoDB as a macOS service, as doing so sets the correct system ulimit values automatically.


- To start/stop MongoDB (i.e. the mongod process) as a macOS service, issue the following:

brew services start mongodb-community@4.2

brew services stop mongodb-community@4.2


- To start/stop MongoDB (i.e. the mongod process) manually as a background process, issue the following:

mongod --config /usr/local/etc/mongod.conf

mongo admin --eval "db.shutdownServer()"
db.getSiblingDB("admin").shutdownServer({ "force":false, "timeoutSecs": 60 })
or
https://docs.mongodb.com/manual/reference/command/shutdown/#dbcmd.shutdown
db.adminCommand({ "shutdown" : 1, "force" : false, timeoutSecs: 60 })

- To verify that MongoDB is running, search for mongod in your running processes:

ps aux | grep -v grep | grep mongod
or view log file: /usr/local/var/log/mongodb/mongo.log.


- To run MongoDB, from a new terminal, issue the following:
mongo


----------------------------- MONGO SHELL:

-instance running on localhost:27017
mongo

-instance running on another port
mongo --port 28015

-on a remote host
mongo "mongodb://mongodb0.example.com:28015" ; mongo --host mongodb0.example.com:28015 ; mongo --host mongodb0.example.com --port 28015

-You can specify the username, authentication database, and optionally the password in the connection string
mongo "mongodb://user@mongodb0.examples.com:28015/?authSource=admin"
mongo --username alice --password --authenticationDatabase admin --host mongodb0.examples.com --port 28015

-Display all databases
show dbs

-Display db in use
db

-Switch database
use <dbname>


-Show Collections in db
db.getCollectionNames()
show collections
show tables


-You can switch to non existing db, creating it while inserting:
use myNewDatabase
db.myCollection.insertOne( { x: 1 } );


-If a collection uses a conflictive name:
db.getCollection("3 test").find()...


-Format printed results
.The db.<collection>.find() method returns a cursor to the results; however, in the mongo shell, if the returned cursor is not assigned to a variable using the var keyword, then the cursor is automatically iterated up to 20 times to print up to the first 20 documents that match the query. The mongo shell will prompt Type it to iterate another 20 times.
db.myCollection.find().pretty()
//mirar multi-line operations y ejemplo siguiente para asignar cursor a un var
print(..), printjson(..)


- Multi-line Operations in the mongo Shell
> if ( x > 0 ) { //si terminamos línea con (, {, [
... count++;
... print (x);
... } //cerramos con el equivalente o, 2 blank lines


var cursor = db.myCollection.find().sort({_id:-1}).limit(10000);
while(cursor.hasNext()){
    printjsononeline(cursor.next());//print(..), printjson(..)
}


- Usar Tab ( ->| ) para autocompletion


-.mongorc.js File
When starting, mongo checks the user’s HOME directory for a JavaScript file named .mongorc.js. If found, mongo interprets the content of .mongorc.js before displaying the prompt for the first time. If you use the shell to evaluate a JavaScript file or expression, either by using the --eval option on the command line or by specifying a .js file to mongo, mongo will read the .mongorc.js file after the JavaScript has finished processing. You can prevent .mongorc.js from being loaded by using the --norc option.


-Exit the Shell
type quit() or use <Ctrl-C>

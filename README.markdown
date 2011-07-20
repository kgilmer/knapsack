
# Knapsack for Apache Felix

Knapsack is a custom launcher for the [Apache Felix OSGi Framework](http://felix.apache.org/site/index.html) with native shell integration and a few default services that run out of the box.  See [http://kgilmer.github.com/knapsack/](http://kgilmer.github.com/knapsack/) for more information about its design.

# Setting up an OSGi-based application
- [Download the knapsack binary](https://leafcutter.ci.cloudbees.com/job/knapsack/lastSuccessfulBuild/artifact/knapsack.jar) from the Jenkins instance running at Cloudbees.
- Run `knapsack.jar` from a designated directory.

```
$ mkdir test && cp knapsack.jar test/ && cd test
$ wget https://leafcutter.ci.cloudbees.com/job/knapsack/lastSuccessfulBuild/artifact/knapsack.jar
$ java -jar knapsack.jar &
[1] 5513
...
INFO: Framework started in 0.144 seconds with activators: [org.apache.felix.log.Activator@44bd928a, org.apache.felix.cm.impl.ConfigurationManager@79dfc547, org.knapsack.Activator@5210f6d3]
```
- The core framework, Knapsack, the Log Service, and the Configuration Admin are now running.  However, they do not show up as seperate bundles because they are run at startup with the framework itself.  Take a look:

```
$ bin/bundles 
ACTIV 	[ 0]org.apache.felix.framework (0.0.0) 	Bundle
```
- Now let's add some bundles to our running instance to add services and functionality.  Knapsack automatically creates a directory for us to add bundles to:

```
$ cd bundle
$ wget http://ftp.jaist.ac.jp/pub/apache//felix/org.apache.felix.http.bundle-2.2.0.jar
$ wget http://ftp.jaist.ac.jp/pub/apache//felix/org.apache.felix.webconsole-3.1.8.jar
$ wget http://ftp.jaist.ac.jp/pub/apache//felix/org.apache.felix.webconsole.plugins.memoryusage-1.0.2.jar
$ wget http://ftp.jaist.ac.jp/pub/apache//felix/org.apache.felix.metatype-1.0.4.jar
$ wget http://ftp.jaist.ac.jp/pub/apache//felix/org.apache.felix.http.jetty-2.2.0.jar
```
This set of bundles will let us run servlets and expose the running framework via a web console.  We need to tell knapsack to start the bundles, not just install them.  We do this by setting the execution bit on the file:

```
$ chmod u+x *
```

- Now we tell knapsack to rescan the bundle directory:

```
$ cd ..
$ bin/update
...
```

- You will see some exceptions in the output, but these are not critical and refer to some missing optional dependencies.  You can confirm that the http server and web console are running with:

```
$ bin/bundles | grep ACT
ACTIV 	[ 0]org.apache.felix.framework (0.0.0) 	Bundle
ACTIV 	[ 1]org.apache.felix.webconsole (3.1.8) 	/tmp/kt/bundle/org.apache.felix.webconsole-3.1.8.jar
ACTIV 	[ 2]org.apache.felix.webconsole.plugins.memoryusage (1.0.2) 	/tmp/kt/bundle/org.apache.felix.webconsole.plugins.memoryusage-1.0.2.jar
ACTIV 	[ 4]org.apache.felix.metatype (1.0.4) 	/tmp/kt/bundle/org.apache.felix.metatype-1.0.4.jar
ACTIV 	[ 5]org.apache.felix.http.jetty (2.2.0) 	/tmp/kt/bundle/org.apache.felix.http.jetty-2.2.0.jar
ACTIV 	[ 6]org.apache.felix.http.bundle (2.2.0) 	/tmp/kt/bundle/org.apache.felix.http.bundle-2.2.0.jar
```
If you direct your web browser to `http://localhost/system/console` you should see the admin interface.  Log in with admin/admin to inspect your runtime environment.

![Felix Web Console](http://kgilmer.github.com/knapsack/images/screenshot1.png)

# Directory Layout

Knapsack will create it's preferred configuration environment if it's not already provided.  After running knapsack the directory should look something like this:

```
$ ls
bin  bundle  cache  configAdmin default felix.conf  knapsack.jar
```

## /bin
This is where the 'native' scripts are stored that can be executed via the system terminal.  There is a hidden file `.knapsack-command.sh` which does the work of storing the randomly generated active port number (so that multiple knapsack instances do not collide), and passing the command line to knapsack via netcat.

## /bundle
This is the default location where bundles are stored.  As mentioned in `felix.conf`, you can set `org.knapsack.bundleDirs` to be any set of directories, scanned in the order they are specified.

Organizing an application is into logical areas of functionality, for example a web application could have `core`, `database`, and `http` directories, each with relevant bundles, making deployment a bit easier.

## /cache
This is where the Felix bundle cache goes.  It is an exploded form of the bundle jars.  By default, this directory is deleted each time Knapsack runs, to ensure that the latest code from bundle directories is always getting executed.

## /configAdmin
Configuration Admin typically keeps its state in the bundle cache.  We want to preserve configuration state across knapsack instances so we store it seperately.

## /default
Startup state can be set in Felix via system properties or Configuration Admin.  Knapsack will load property files and Configuration Admin dictionaries from the default directory.  Any file ending with ".properties" will be treated as a properties file, otherwise the filename will be assumed a PID and a Configuration will be created with its name/value pairs.

Since all files in this directory are properties, one can have different files for different services or bundles, rather than one giant property file.  This also is nice for installers, when a set of properties corresponds to a bundle or aspect of functionality.

## /felix.conf
A default felix configuration is generated if it doesn't already exist.  This lets the user tune the startup settings that are related to the framework internals.  Some knapsack settings are also available.

# System shell features

Check the log with:

```
$ bin/log 
...
07.20 09:29:49 	INFO    	BundleEvent INSTALLED 	[ 2]org.apache.felix.webconsole.plugins.memoryusage (1.0.2)
07.20 09:29:49 	INFO    	BundleEvent INSTALLED 	[ 3]org.apache.felix.deploymentadmin (0.9.0)
07.20 09:29:49 	INFO    	BundleEvent INSTALLED 	[ 4]org.apache.felix.metatype (1.0.4)
07.20 09:29:49 	INFO    	BundleEvent INSTALLED 	[ 5]org.apache.felix.http.jetty (2.2.0)
...
```

Grep for errors:

```
$ bin/log | grep ERR
07.20 09:29:49 	ERROR   	FrameworkEvent ERROR 	[ 3]org.apache.felix.deploymentadmin (0.9.0)
07.20 09:30:08 	ERROR   	Unable to start /tmp/kt/bundle/org.apache.felix.deploymentadmin-0.9.0.jar. 	[ 0]org.apache.felix.framework (0.0.0)
```

See which bundles are running with:

```
$ bin/bundles 
ACTIV 	[ 0]org.apache.felix.framework (0.0.0) 	Bundle
ACTIV 	[ 1]org.apache.felix.webconsole (3.1.8) 	/tmp/kt/bundle/org.apache.felix.webconsole-3.1.8.jar
ACTIV 	[ 2]org.apache.felix.webconsole.plugins.memoryusage (1.0.2) 	/tmp/kt/bundle/org.apache.felix.webconsole.plugins.memoryusage-1.0.2.jar
INSTL 	[ 3]org.apache.felix.deploymentadmin (0.9.0) 	/tmp/kt/bundle/org.apache.felix.deploymentadmin-0.9.0.jar
ACTIV 	[ 4]org.apache.felix.metatype (1.0.4) 	/tmp/kt/bundle/org.apache.felix.metatype-1.0.4.jar
```

Shut the framework down with:

```
$ bin/shutdown-knapsack 
INFO: Bundle org.apache.felix.framework [0] OSGi framework is shutting down due to user request via shell.
...
```

Show the OSGi available services and binding relationships:

```
$ bin/services -d
[23] 	javax.servlet.http.HttpServlet 	[ 6]org.apache.felix.http.bundle (2.2.0)
[17] 	org.osgi.service.cm.ManagedService 	[ 1]org.apache.felix.webconsole (3.1.8)
	Used by [ 0]org.apache.felix.framework (0.0.0)
[16] 	org.apache.felix.webconsole.ConfigurationPrinter 	[ 1]org.apache.felix.webconsole (3.1.8)
```

Get help on available commands and parameters:

```
$ bin/help 
bundles [-b (brief)] 	Get list of OSGi bundles installed in the framework.
printconfig          	Print the Java system configuration.
help                 	Print table of currently available commands.
shutdown-knapsack -f (force) 	Stop all bundles and shutdown OSGi runtime.
services [-b (brief)] [-d (show dependencies)] [-p (show properties)] 	Display OSGi services active in the framework.
log [-b (brief)]     	Print OSGi log.
headers [bundle id]  	Print bundle headers.
update               	Rescan the bundle directory or directories and update bundlespace accordingly.
```

# License

Like Felix itself, knapsack is Apache 2.0 licensed.
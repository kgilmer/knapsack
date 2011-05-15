
# Knapsack for Apache Felix

Knapsack is a custom launcher for the [Apache Felix OSGi Framework](http://felix.apache.org/site/index.html) with a filesystem-based management library, and a few default services that run out of the box.  Knapsack was born out the desire to remove some of the complexities that ship with more full featured launchers, and is designed for small and simple projects.  The problem that knapsack tries to address is to get an OSGi framework up and running with a minimum of fiddling.  Knapsack is both a little less and a little more than Felix's built-in launcher.  It's less in that it does not use runlevels and all bundle install/start configuration is done via the filesystem.  It is a little more because a few bundles are started by default.  Knapsack's design is a result of the following opinions.

*OSGi Shells are usually overkill for what a framework admin needs to do.*  Most of the time, we ask **"Is my bundle running?"** or **"is the HTTP service available?"**.  OSGi shells are a way of answering that question, but are worlds into themselves and the source of unnecessary complexity.  Based on ideas from the [OSGiFS prototype](http://kgilmersden.wordpress.com/2010/12/14/a-shell-less-osgi-shell/), Knapsack creates two filesystem pipes, one for reading and one for writing.  To see service or bundle state, simply cat the contents of `/info`.  To shutdown the framework, simply write `shutdown` to `/control`.  Bouncing a bundle or restarting the framework work similarly.  To install a bundle, just put it in `/bundle`.  Want it started by default?  Set the execution bit on the bundle file.  Done.

*The storage dir is a source of pain when debugging bundles.*  By default Knapsack will not keep bundles from pre-existing runtime sessions.  Non-executable state (ConfigAdmin) is stored elsewhere (/configAdmin) so that it doesn't get clobbered every time the framework starts.  As a result, there is always only one place that bundle code comes from, /bundle.

*Setup of an OSGi-based application is too hard.*  Typically, running a program goes something like `$ myprogram <enter>` Knapsack strives for this level of simplicity via conventions and default behavior.  On start, knapsack will create it's configuration if it does not already exist.

*Some things are better have around from the get-go.*  OSGi, as a component system, is designed for extreme flexibility.  Almost all services are optional.  However life is a lot easier if you have a few services around.   [LogService, LogReader](http://felix.apache.org/site/apache-felix-log.html), and [ConfigAdmin](http://felix.apache.org/site/apache-felix-config-admin.html) are immediately available.  These services start with the framework, so from a bundle's perspective they will always exist.  In this regard, Knapsack is similar to [Apache Karaf](http://karaf.apache.org/), in that it adds a set of bundles to provide a sort of application platform.  The difference is Karaf has more of a server-oriented enterprise platform design, and as a result is more complex (and feature rich).

*Well-known filesystem operations are best for configuration storage and state modification.*  By overloading the concept of an executable file, bundles become executables or libraries.  Output is grep-friendly and accessing OSGi runtime state info from scripts is straight-forward.

*Filesystem polling is more trouble than it's worth.*  In OSGiFS I experimented with automatic ways of syncing the filesystem state with the bundle state.  However it turned out that this caused confusion in debugging sessions, and more explicit control is better.  After making changes in the `/bundle` directory, run `/bin/knapsack-rescan.sh` or `echo rescan > control`.

Most of the above behavior is the default behavior.  As Knapsack is just a minimal facade to Felix, adding bundles (shell, webadmin, etc.) and changing the configuration (`onFirstInit`, etc.) allows for much flexibility if a default behavior is undesired.  

# Getting Started
1. Run the knapsack jar from a designated directory.

```
$ java -jar knapsack.jar &
[1] 5513
INFO: Framework started in 0.144 seconds with activators: [org.apache.felix.log.Activator@44bd928a, org.apache.felix.cm.impl.ConfigurationManager@79dfc547, org.knapsack.Activator@5210f6d3]
```

Knapsack will create it's preferred configuration environment if it's not already provided.  After running the directory should look something like this:

```
$ ls
bin  bundle  bundleCache  configAdmin  control  info  knapsack.conf  knapsack.jar
```

Check the log with:

```
$ bin/knapsack-log.sh 
05.15 17:43:01 INFO   	 ServiceEvent REGISTERED	 org.apache.felix.framework
05.15 17:43:01 INFO   	 ServiceEvent REGISTERED	 org.apache.felix.framework
05.15 17:43:01 INFO   	 ServiceEvent REGISTERED	 org.apache.felix.framework
05.15 17:43:01 INFO   	 ServiceEvent REGISTERED	 org.apache.felix.framework
05.15 17:43:01 INFO   	 Knapsack 0.1.0 starting in /tmp/kn2	 org.apache.felix.framework
05.15 17:43:01 INFO   	 FrameworkEvent STARTED	 org.apache.felix.framework
05.15 17:43:01 INFO   	 BundleEvent STARTED	 org.apache.felix.framework
```

See which bundles are running with:

```
$ bin/knapsack-bundles.sh 
org.apache.felix.framework
```

## /bin
This directory contains some script files for common operations such as shutting down the framework or reading the log.

## /info
This is the output pipe by which information about the OSGi runtime is exposed.

## /control
This is the input pipe which allows the framework administrator to change the state of the OSGi runtime.

## /bundle
Like the standard Felix launcher, this is where bundles go.  If a bundle has executable permission, knapsack will start it.  Otherwise it will remain in the `INSTALLED` state.

## knapsack.conf
The configuration file used to set Felix properties on launch.

## /configAdmin
The root directory for storage of ConfigAdmin's data.   

## /bundleCache
Renamed from `felix-cache`, this is the storage dir, and by default it is deleted on each framework start.

# License

Knapsack is Apache 2.0 licensed.  Portions of pre-existing dependent code are BSD 3-clause.
# Work in Progress - ZOOM

*ZOOM* is a module for OpenDaylight that is capable of identifying heavy hitting flows by performing a dissection of the IP range. It can be downloaded from the [releases](https://github.com/lsinfo3/zoom-odl/releases/) page.

## Building
The module can be built using [Maven 2](https://maven.apache.org/).
By running

```console
$ mvn clean install
```

or a respective command from the *ZOOM* root folder, the built module will end up in a `./target/zoom-X.X.X.jar` file.

## Dependencies
The current version of *ZOOM* uses [tcpreplay-edit](https://github.com/appneta/tcpreplay) to replay a PCAP trace file to a switch connected to the OpenDaylight controler. Either a virtual OpenVSwitch in [Mininet](https://github.com/mininet/mininet/) or OpenFlow-capable hardware switches may be used. (_NOTE: Several hardware switches have been tested and posed different issues that need to be resolved. Results may vary strongly._)

## Adding the *ZOOM* Module to OpenDaylight
The current version of the ZOOM module (Version 1.0) is compatible with OpenDaylight Hydrogen 1.0, which can be downloaded from http://www.opendaylight.org/software/release-archives.

In order to run OpenDaylight including the *ZOOM* module, copy the built `zoom-X.X.X.jar` file into `opendaylight/plugins/` and run OpenDaylight with the startup script according to the used platform (_NOTE: The module has only been tested under Linux_). After the first installation of the module, it needs to be manually activated from the osgi console. To do so simply follow the steps below.

```console
# For Linux systems to start OpenDaylight
$ ./run.sh
# Once OpenDaylight is launched and ready, the osgi> prompt will show up.
osgi> 
# Issuing the command ss will show a list of available modules and their respective state. The output can be filtered by providing a search string to ss.
osgi> ss zoom
"Framework is launched."


id	State       Bundle
169	RESOLVED    org.opendaylight.controller.zoom_1.0.0
osgi>
# Issuing the start command providing the module ID will start the module and the state will change to active
osgi> start 169
2015-06-18 11:52:57.235 MESZ [Gogo shell] INFO  o.o.controller.zoom.internal.Zoom - Initialized
2015-06-18 11:52:57.236 MESZ [Gogo shell] INFO  o.o.controller.zoom.internal.Zoom - Started
osgi> ss zoom
"Framework is launched."


id	State       Bundle
169	ACTIVE      org.opendaylight.controller.zoom_1.0.0
osgi>
```

From now on, the module is loaded and will be started automatically with every start of OpenDaylight. The module can be stopped by issuing the `stop <module id>` command. In order to update the module (e.g. after the `zoom-X.X.jar` file has been rebuilt due to changes to the code) the old module's `.jar` file can be overwritten even while OpenDaylight is running. OpenDaylight will reload it automatically. 

## Using the *ZOOM* Module
Once all dependencies are met and the *ZOOM* module is loaded, the algorithm is almost ready to run.
As root permissions are required for `tcpreplay`, a sudo password has to be provided in the seperate implementation classes (ZoomBase.java and ZoomTT.java). **Note that the module has to be rebuilt after the password has been set.**

In order to run the algorithms provided by the module, two modes are provided.

### Interactive Mode
In interactive mode, all parameters need to be provided via the osgi console start the algorithm. Each implementation features its own interactive call. Following are examples on how to call the different algorithms:

```console
osgi> zoomBaseInteractive -trace /home/trace/trace.pcap -nflows 2 -ntop 1 -t 2 -ncycles 1 -offset 5
```

This will run the base version of the algorithm with the according parameters. According to this, the *ZOOM-TT* implementation can be run.

```console
osgi> zoomTTInteractive -trace /home/trace/trace.pcap ...
```

### Automatic Mode
In automatic mode, all parameters get specified directly in the [org.opendaylight.controller.zoom.internal.Zoom](https://github.com/lsinfo3/zoom-odl/blob/58c2fd0a44feec265fd4a554e97dced8581ffad6/src/main/java/org/opendaylight/controller/zoom/internal/Zoom.java#L133-L191) class. This allows to run the algorithm multiple times with different parameter combinations automatically.

In order to run the algorithm in automatic mode, all parameter combinations that should be evaluated have to be specified. An example of a reasonable set of combinations is provided [in the file](https://github.com/lsinfo3/zoom-odl/blob/58c2fd0a44feec265fd4a554e97dced8581ffad6/src/main/java/org/opendaylight/controller/zoom/internal/Zoom.java#L133-L191).

## License

The ZOOM module is published under the [EPL-1.0 license](https://www.eclipse.org/legal/epl-v10.html).


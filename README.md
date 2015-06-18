# Work in Progress - ZOOM

*ZOOM* is a module for OpenDaylight that is capable of identifying heavy hitting flows by performing a dissection of the IP range.

## Building
The module can be built using [Maven 2](https://maven.apache.org/).
By running `mvn clean install` or a respective command from the ZOOM root folder the source code will be packaged into a `zoom-X.X.jar` file that will be created within the `./target/` subfolder.

## Dependencies
The current version of zoom uses `tcpreplay-edit` to replay a pcap traffic file that is replayed to a switch that is connected to the OpenDaylight controler. Either a virtual OpenVSwitch in `mininet` or OpenFlow capable hardware switches may be used. (_NOTE: Several hardware switches have been tested and posed different issues that need to be resolved. Results may vary strongly._)

## Adding the ZOOM Module to OpenDaylight
The current version of the ZOOM module (Version 1.0) runs with the legacy version of OpenDaylight Hydrogen 1.0 which can be downloaded from http://www.opendaylight.org/software/release-archives.

In order to run OpenDaylight including the ZOOM module simply copy the built `Zoom-X.X.jar` file into `opendaylight/plugins/` and run OpenDaylight with the startup script according to the used platform (_NOTE: The module has only been tested under Linux_). After the first installation of module it need to be manually activated from the osgi console. To do so simply follow the steps below.

```
# For Linux systems to start OpenDaylight
$ ./run.sh #
# Once OpenDaylight is launched and ready the osgi prompt will show up.
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
From now on the module is loaded and will be started automatically with every start of OpenDaylight. The module can be stopped by issuing the `stop <module id>` command. In order to update the module (e.g. after the `Zoom-X.X.jar` file has been rebuilt due to changes to the code) the old module .jar file can be overwritten even while OpenDaylight is running. OpenDaylight will reload the module automatically. 

## Using the ZOOM Module
Once all dependencies are met and the ZOOM module is loaded the algorithm is almost ready to run.
Before the module can be run, a sudo password has to be provided in the seperate implementation classes (ZoomBase.java and ZoomTT.java). This is needed since tcpreplay-edit requires root access to correctly replay the traffic file. **Note that the module has to be rebuilt after the password has been set.**

In order to run the algorithms provided by the module ZOOM provides 2 different modes.

### Interactive Mode
In interactive mode all parameters need to be provided via the osgi console in order to run the algorithm. Each provided implementation features its own interactive call. Following are examples on how to call the different algorithms.

```
osgi> zoomBaseInteractive -trace /home/trace/trace.pcap -nflows 2 -ntop 1 -t 2 -ncycles 1 -offset 5
```
This will run the base version of the algorithm with the according parameters. According to this, the ZoomTT implementation can be run.
```
osgi> zoomTTInteractive -trace /home/trace/trace.pcap ...
```

### Automatic Mode
In automatic mode all parameters get specified directly in the Zoom.java main class. By this it is possible to run the algorithm multiple times with different parameter combinations automatically.

In order to run the algorithm in automatic mode the `Zoom.java` main needs to contain all parameter combinations that should be used. An example of a sensible set of combinations is provided by default in the file. The parameter combinations are created and executed in
```
public void _zoomAutomated(CommandInterpreter ci)
```
## License

The ZOOM module is published under the [EPL-1.0 license](https://www.eclipse.org/legal/epl-v10.html).


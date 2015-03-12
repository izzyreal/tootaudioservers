Using AudioServer Service Provision with toot2

# Introduction #

AudioServer related services are represented by the seperate concerns AudioServer, AudioServerConfiguration and various UIs as specified by Toot2. The details below rely on support classes from toot2, tootaudioservers currently only contains implementations.

# Details #

An instance of an AudioServer may be instantiated explicitly, as previously, but this requires that the application knows what concrete implementations are available, which somewhat defeats the point of service provision.

Assuming that the application knows the name of the server that was last used, the user can accept that name or choose an alternative server name using:
```
    String serverName = ...   // previously used server name
    serverName = AudioServerChooser.showDialog(serverName);
    if ( serverName == null ) // cancel was pressed, terminate application
    // persist serverName here for next time
```
Which displays a dialog with server name selection and OK and Cancel buttons, and terminates the application if Cancel was pressed. If OK was pressed serverName is the name of the server selected by the user.

A named AudioServer can then be instantiated and initialised using:
```
    AudioServer server = AudioServerServices.createServer(serverName);
```

It may be that the instantiated server requires implementation specific setup prior to use. This requires such setup is available as Properties. Typically this might be used to set the server sample rate prior to using the server.
```
    final AudioServerConfiguration serverSetup = 
        AudioServerServices.createServerSetup(server);
    if ( serverSetup != null ) {
        serverSetup.applyProperties(properties);
        serverSetup.addObserver(
            new Observer() {
                public void update(Observable obs, Object obj) {
                    serverSetup.mergeInto(properties);
                    // store changed properties here
                }
            }
        );
    }
    AudioServerUIServices.showSetupDialog(server, serverSetup);
```
Which obtains an implementation specific AudioServerConfiguration and, assuming it is non-null, applies the existing properties and observes and stores any changes due to the user interacting with the modal setup dialog.

The instantiated server also requires implementation specific configuration while running.
```
    AudioServerConfiguration serverConfig = 
        AudioServerServices.createServerConfiguration(server);
    serverConfig.applyProperties(properties);
    serverConfig.addObserver(
        new Observer() {
            public void update(Observable obs, Object obj) {
                serverConfig.mergeInto(properties);
                // store changed properties here
            }
        }
    };
    JComponent ui = AudioServerUIServices.createServerUI(server, serverConfig);
```
Which obtains an implementation specific AudioServerConfiguration, applies existing properties and observes and stores any changes due the user interacting with the UI. The UI is not displayed at this time, it is simply available for later use.
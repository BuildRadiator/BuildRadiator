# Here is an example of a build radiator

![Radiator pic](https://cloud.githubusercontent.com/assets/82182/26152799/8c69e2f0-3ad6-11e7-8294-62196ebad748.png)

# An online service for all

[buildradiator.org](https://buildradiator.org)

The service above will hold radiator state for 10 builds, per radiator. You can go and make your own radiator on
it, and then place a TV/monitor on a wall somewhere to show that page in a browser. You could choose a consumer device like a
[SmartTV, Amazon Fire TV stick, AppleTV](//github.com/BuildRadiator/BuildRadiator/wiki/Consumer-Displays), etc. You can dedicate a 
regular PC too, but you need to solve the "how to leave it logged in, and the screen-saver off" problem yourself.

## Security through obscurity

We admit, `buildradiator.org` is [security through obscurity](https://en.wikipedia.org/wiki/Security_through_obscurity) proposition.

Every time a radiator is created, there's a **random code** that is generated. An example is `ueeusvcipmtsb755uq` (made for the 
demo radiator that can't be updated). Unless someone knows the code, they can't see your radiator. There is no list of radiators
either. If you lost your radiator code, just made a new one and the old one will be deleted after a couple of weeks of non-use.

The server is never given parts of the URL to the right of the `#`.

# Creating Radiators

[See Wiki Page](//github.com/BuildRadiator/BuildRadiator/wiki/Creating-a-radiator). Also detailed there is info on
locking the radiator to certain IP addresses.

# Updating the radiator from your CI daemon
 
[See Wiki Page](//github.com/BuildRadiator/BuildRadiator/wiki/Updating-build-step-changes-from-CI). Also detailed
there is how a secret is passed to buildradiator.org that ensures that only approved CI jobs update build statuses. 

# Navigating to your radiator

Type `https://buildradiator.org/r#<the radiator ID frome the create step>/A_Long_Decription_For_Your_Radiator`

Be aware that a HTML page is loaded in the browser for `https://buildradiator.org/r` and that in turn 
seeks a JSON payload -  `https://buildradiator.org/r/<the radiator ID frome the create step>`. Any other attribute
to the right of the `#` is not passed to the browse so you can 
knock yourself out with secret project names, etc. 
[Read more on the wiki page](https://github.com/BuildRadiator/BuildRadiator/wiki/Setting-the-title-and-expanding-step-codes-in-the-UI)

# Putting a screen up in your guest WiFi

[See Wiki Page on consumer displays](//github.com/BuildRadiator/BuildRadiator/wiki/Consumer-Displays)

# Building the application yourself

Current build status: ![](https://circleci.com/gh/BuildRadiator/BuildRadiator.png?style=shield&circle-token=64772cdbf7a8b6c2c6bbfb6a8f52802e0b662a24)

[Build Radiator for this repo](https://buildradiator.org/r#b7n63m6hcb9sm2ttdn/Build_Radiator_DotOrg_Master)

Command to to run build:

```
mvn clean install
```

In about 30 seconds, the build does: 

1. compile, 
2. unit test compile, 
3. unit test invocation, 
4. integration test invocation, 
5. functional test invocation (WebDriver)
6. distribution creation (uberjar style jar)

## Build prerequsites

1. Chromedriver.exe for your platform (homebrew has it)
2. Maven 3+

## Deploying to Google AppEngine

This is to the "Flexible" AppEngine for Java variant, and not the traditional one. 
It also needs a 'datastore' that is connected to the AppEngine app/project, but the schema is setup automatically on boot.

```
mvn -Pgae -DskipTests appengine:deploy
```

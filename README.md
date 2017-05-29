# Here is an example of a build radiator

![Radiator pic](https://cloud.githubusercontent.com/assets/82182/26152799/8c69e2f0-3ad6-11e7-8294-62196ebad748.png)

# An online service for all

[buildraditor.org](https://buildraditor.org)

The service above will hold radiator state for 10 builds, per radiator. You can go and make your own radiator on
it, and then place a TV/monitor on a wall somewhere to show that page in a browser. You could choose a 
[SmartTV](//github.com/paul-hammant/buildradiator/wiki/Compatible-SmartTVs) or an 
[Amazon Fire TV stick](//github.com//paul-hammant/buildradiator/wiki/Amazon-Fire-TV-Stick) with a conventional TV, or an 
[AppleTV](//github.com//paul-hammant/buildradiator/wiki/AppleTV). All of those are for TV solutions. You can dedicate a 
regular PC too, but you need to solve the "how to leave it logged in, and the screen-saver off" problem yourself.


## Security through obscurity

We admit, `buildradiator.org` is [security through obscurity](https://en.wikipedia.org/wiki/Security_through_obscurity) proposition.

Every time a radiator is created, there's a **random code** that is generated. An example is `ueeusvcipmtsb755uq` (made for the 
demo radiator that can't be updated). Unless someone knows the code, they can't see your radiator. There is no list of radiators
either. If you lost your radiaor code, just made a new one and the old one will be deleted after a couple of weeks of non-use.



### The Server side doesn't know the "name" of your radiator

That's handled in the browser exclusively:

Compare [https://buildradiator.org/r#ueeusvcipmtsb755uq/Goldman_Sachs_Top_Secret_Quantum_Sniping_Automatic_Trader](https://buildradiator.org/r#ueeusvcipmtsb755uq/Goldman_Sachs_Top_Secret_Quantum_Sniping_Automatic_Trader) to
[https://buildradiator.org/r#ueeusvcipmtsb755uq/These_Are_Not_The_Droids_You_Are_Looking_For](https://buildradiator.org/r#ueeusvcipmtsb755uq/These_Are_Not_The_Droids_You_Are_Looking_For).

The server is never given parts of the URL to the right of the `#`.

# Creating Radiators

[See Wiki Page](//github.com//paul-hammant/buildradiator/wiki/Getting-Started)

# Updating the radiator from your CI daemon
 
[See Wiki Page](//github.com//paul-hammant/buildradiator/wiki/Updating-the-status-of-a-build)

There's an extension for Maven, that does the step updates, too: [Build Radiator Event Spy](//github.com/paul-hammant/BuildRadiatorEventSpy)

# Navigating to your radiator

Type `https://buildradiator.org/r#<the radiator ID frome the create step>/A_Long_Decription_For_Your_Radiator`

Be aware that a HTML page is loaded in the browser for `https://buildradiator.org/r` and that in turn 
seeks a JSON payload -  `https://buildradiator.org/r/<the radiator ID frome the create step>`. Any other attribute
to the right of the `#` is [not passed to the browser](https://en.wikipedia.org/wiki/Fragment_identifier) so you can 
knock yourself out with secret project names, etc. 

# Building the application yourself

```
mvn clean install
```

The build does: 

1. compile, 
2. unit test compile, 
3. unit test invocation, 
4. integration test invocation, 
5. functional test invocation (WebDriver)
6. distribution creation (uberjar style)

## Prerequsites

1. Chromedriver.exe for your platform (homebrew has it)
2. Maven 3+

## Deploying to Google AppEngine

This is to the "Flexible" AppEngine for Java variant, and not the traditional one.

```
mvn -Pgae -DskipTests appengine:deploy -Dgae_appId=<appID of your app on GoogleAppEngine>
```
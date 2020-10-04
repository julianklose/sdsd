# Set up your IDE
1. Download and install the Java JDK version 14: https://adoptopenjdk.net/?variant=openjdk14&jvmVariant=openj9
2. Download and install the latest version of the Eclipse IDE.
3. Open the Eclipse IDE and select your preferred workspace directory.
4. Clone this repository into your Eclipse workspace:
`$ git clone https://git.opendfki.de/SDSD/prototype.git`
5. In Eclipse, navigate to `File -> Import` and select `Maven -> Existing Maven Projects`. Click on `Next >`.
6. Select the folder `prototype` as `Root Directory`. It should be located inside your Eclipse workspace directory, after you've cloned this repository.
7. Click on `Select All` to mark all projects inside the `Projects` window for import. At this point you have the option to organize the Maven projects as working sets. Click on `Next >`.
8. Eclipse now starts to import the selected Maven projects, which rely on the external dependencies listed in XML files called `pom.xml`. Those contain URLs to the corresponding `.jar` archives  which are currently being downloaded in the background. This may take a while, please be patient.
9. In the package explorer, right click on the project `website`, hover your mouse over `Run As` and select `Run Configurations`.
10. On the left, right click on `Java Application`, select `New Configuration` and enter `SDSD Website` as the name of the configuration.
11. Stay in the `Main` tab and select `website` as `Project` by clicking `Browse`.
12. Copy and paste `de.sdsd.projekt.prototype.Main` into the `Main class` input field.
13. In the same window, navigate to the `Arguments` tab and copy-paste the following into the  `Program arguments` textfield:
`-H "https://app.sdsd-projekt.de" -c "settings.json"` 
14. Copy and paste the following line into the `VM arguments` textfield:
`-DredirectToLocalhost="https://app.sdsd-projekt.de/rest/onboard" -DdetailedDebugMode=true -DdetailedDebugModeRaw=false`
15. In the same tab, change the `Working directory` of the `website` project to a preferred location by selecting `Other -> File System`.
16.  Copy the `view` folder from `prototype/website/` into the working directory of the `website` project.
17. Copy the `parser`  folder containing a bunch of `.jar` archives into the same directory as the `view` folder. At this point, the `parser` folder inside of `prototype/website/` does not contain the fully built `.jar` archives.
18. Copy the credentials file `settings.json` into the working directory of the `website` project.
19. Install the key-value store Redis using its default configuration. After the installation has finished, the database will be running at 127.0.0.1:6379.

# Folders
* prototype - SDSD server (website)
* agrirouter - agrirouter connection code used by prototype project
* isoxml-parser/hs-osna-nodejs - ISOXML parser from Hochschule Osnabrück (Nodejs)
* isoxml-parser/igreen-isoxml - ISOXML parser from iGreen Project (Java)
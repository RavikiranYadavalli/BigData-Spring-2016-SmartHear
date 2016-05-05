This repository contains the source and documentation for the project named SmartHear. As the name suggests the project aims to provide a smart hearing aid in the form of smart phone for specially abled people. The basic idea behind the application is to record the audio around the user and provide the useful information extracted form it to the user. The user can also alter the frequency range of the sound using the pass filters to suit his or hear hearing ability. The workflow The future extension is as follows
1. Train the spark model using the data collected as features from Android device as well as external resources.
2. Sound detection to start the application. Then the recording of the sound and selection of context information. 
3. Features extracted from sound would be sent to the spark server for analysis.
4. Based on spark MLlib prediction the user is notified about the sound detected as an Android notification. 
The technologies used in the project are
1. Android SDK using Android Studio.
2. Spark MLlib.
3. Java socket programming.
4. The programming langauge at the client end was Java
5. The spark MLlib training and prediction was done using Scala.

JAduio was used to extract the features from sound stream or files.  Ref : http://jaudio.sourceforge.net/

Following is the video link for the demo of the project. Please refer to the documentation for further details on the project.
https://www.youtube.com/watch?v=bhRDAIpw78k

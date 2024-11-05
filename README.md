# Storage Application
- [Usage](#usage)

## Usage
### Getting Started with the Storage Application
To get started with the Storage Application, follow these steps:

1. Clone the repository:
First, clone the repository to your local machine using Git:
   ```bash
   https://github.com/JerwinCauilan/storageapp.git
3. Navigate to the project directory:
Change the directory to the cloned project:
    ```bash
    cd storage
3. Open the Project in Android Studio:
* Open the Project in Android Studio
* Navigate to the cloned project directory and select it.
4. Configure the Android App
Ensure your project is configured correctly:
* SDK Version: The app requires Android SDK version 30 or higher.
* Gradle Sync: Android Studio will prompt you to sync the project with Gradle once the project is open. Click Sync Now to resolve any dependencies.
5. Set Up an Emulator or Use a Physical Device
You can either run the app on an Android Emulator or a Physical Device. Below are instructions for both options:
Option 1: Set Up an Android Emulator
If you don't have an Android device, you can use the Android Emulator to run the app. Here's how to set it up:
1. Open AVD Manager in Android Studio:
* In Android Studio, go to Tools > AVD Manager (Android Virtual Device Manager).
2. Create a New Virtual Device:
* Click on Create Virtual Device.
* Choose a device from the list of available options
3. Select System Image:
* After selecting the device, you'll need to choose a system image (Android OS version).
4. Configure Emulator Settings:
* You can leave the default settings for now
5. Launch the Emulator:
* After creating the virtual device, click the Play button next to the device in the AVD Manager to launch the emulator.
6. Run the Application:
* With the emulator running, return to Android Studio and click the Run button

Option 2: Run on a Physical Device
If you prefer to use your physical Android device for testing, follow these steps:
1. Enable Developer Options on Your Device:
* Open the Settings app on your Android device.
* Scroll down and tap About phone.
* Tap Build number to enable Developer options.
2. Enable USB Debugging:
* Go back to Settings and tap Developer options.
* Enable USB debugging by toggling the switch.
3. Connect Your Device via USB:
* Use a USB cable to connect your Android device to your computer.
4. Select Your Device in Android Studio:
* Once your device is connected, Android Studio will detect it. In the Select Deployment Target window, choose your physical device from the list.
5. Run the Application:
* Click the Run button in Android Studio, and the app will be installed and launched on your physical device.

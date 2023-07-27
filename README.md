# Balloon
# Balloon Experiment

This repository contains the code for the Balloon Experiment conducted by students at Ariel University. The experiment involved using the LORA device and the Mashtastic application to send messages to an Android device attached to a balloon. The Android device was equipped with a custom application that we developed specifically for this experiment.

## Experiment Description

The goal of the experiment was to test the functionality of the LORA device and the Mashtastic application in transmitting messages to the Android device on the balloon. We wanted to explore the capabilities of the application in capturing and storing media (photos and videos) received from the Mashtastic messages.

The experiment involved the following steps:
1. Setting up the LORA device and connecting it to the Mashtastic application.
2. Attaching an Android device with the custom application to a balloon.
3. Sending messages from the Mashtastic application to the Android device using the LORA device.
4. Testing the functionality of the custom application in capturing and storing media based on the received messages.

## Repository Structure

The repository is structured as follows:
- `app`: This directory contains the source code for the custom Android application.
- `screenshots`: This directory contains screenshots of the application in action.
- `videos`: This directory contains video recordings captured by the application.
- `LICENSE`: The license file for this repository.
- `README.md`: This file, providing an overview of the experiment and the application.

## Getting Started

To replicate or further develop the experiment, follow these steps:

1. Clone this repository to your local machine using the following command: git clone https://github.com/Segev955/Balloon.git

2. Open the `app` directory in Android Studio or your preferred integrated development environment (IDE).

3. Build and run the application on an Android device with the required permissions and hardware capabilities.

4. Install the Mashtastic application on another device and set up the LORA device for communication.

5. Send messages using the Mashtastic application and observe the custom application's behavior in capturing and storing media.

## Usage of the Application

The application developed for the balloon experiment allows receiving messages from Meshtastic application during the experiment. When the following messages are received, the application performs the corresponding actions:

<b> Photo</b> - When a "Photo" message is received, the application activates the device's camera and captures a photo. The captured photo is stored in the device's internal memory.

<b>Video</b> - When a "Video" message is received, the application activates the device's camera and starts recording a video for 5 seconds. The recorded video is stored in the device's internal memory.

<b>Video-x</b> - When a "Video-x" message is received, the application activates the device's camera and starts recording a video for x seconds (replace x with the desired number of seconds). The recorded video is stored in the device's internal memory.

<b>Gallery</b> - When a "Gallery" message is received, the application refreshes the built-in gallery of the device. This action displays the newly captured or recorded media in the gallery.

These functions allow the user to receive and view photos and videos from the experiment.

## Contributing

We welcome contributions to enhance the functionality and performance of the custom application. If you would like to contribute, please follow these steps:

1. Fork this repository and clone it to your local machine.

2. Make the necessary changes or additions to the code.

3. Test your changes thoroughly.

4. Create a pull request, clearly describing the changes you have made.

We appreciate your contributions and feedback!

credit: Rony Ronen

# Iterack

![Iterack Logo](screenshots/iterack_logo.png)

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Java-007396?logo=java&logoColor=white)



## Description
**Iterack** is a native Android application designed to streamline daily productivity through an intuitive task management interface. Unlike complex project management tools, Iterack focuses on personal efficiency, offering a clean way to organize tasks by category, track progress with visual insights, and manage deadlines via a built-in calendar planner.

The project is inspired by concepts from **Personal Informatics systems**, where collected data is used to support self-reflection and behavior improvement. Unlike basic checklist apps, Iterack emphasizes **reflection, insight generation, and long-term productivity improvement**.

## Table of Contents
* [Visuals](#visuals)
* [Key Features](#key-features)
* [Tech Stack](#tech-stack)
* [Installation](#installation)
* [Usage](#usage)
* [License](#license)
* [Project Status](#project-status)
* [Contact](#contact)
* [Acknowledgements](#acknowledgements)

## Visuals

| **1. Get Started** | **2. Secure Login** |
|:---:|:---:|
| <img src="screenshots/onboarding.png" width="250" /> | <img src="screenshots/login.png" width="250" /> |
| *Smooth Onboarding* | *Authentication Screen* |

| **3. Dashboard** | **4. Planner** |
|:---:|:---:|
| <img src="screenshots/dashboard.png" width="250" /> | <img src="screenshots/planner.png" width="250" /> |
| *Task Overview* | *Structured Task Planning View* |

| **5. AI Insights** | **6. Settings** |
|:---:|:---:|
| <img src="screenshots/insights.png" width="250" /> | <img src="screenshots/settings.png" width="250" /> |
| *AI-Based Analysis* | *Custom Preferences* |

## Key Features
* **Smart Dashboard:** Real-time overview of daily progress and task groups (Work, Personal, Health, Study).
* **Calendar Planner:** A visual monthly calendar to view and manage upcoming deadlines.
* **AI-Based Productivity Insights:** Generates summaries and suggestions based on user task completion data.
* **Progress Tracking:** Displays task completion patterns and consistency over time.
* **Authentication and Cloud Sync:** Secure login and real-time data synchronization using Firebase.
* **App Lock Security:** Provides an additional layer of protection by securing the app using device-level authentication to prevent unauthorized access.

## Tech Stack
* **Language:** Java
* **Frontend:** XML Layouts, Material Design Components
* **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
* **Backend Services:** Firebase Authentication, Cloud Firestore
* **AI Integration:** Prompt-based AI analysis (Gemini AI API)
* **Tools:** Android Studio, Git, GitHub

## Installation
To run this project locally, you will need **Android Studio**.

1.  **Clone the repository**
    ```bash
    git clone [https://github.com/monitbisht/iterack.git](https://github.com/monitbisht/iterack.git)
    ```
2.  **Open in Android Studio**
    * Go to `File` -> `Open` -> Select the `Iterack` folder.
3.  **Sync Gradle**
    * Allow Android Studio to download the necessary dependencies.
4.  **Run the App**
    * Connect a physical Android device via USB (Debugging enabled) or use the Android Emulator.
    * Press **Shift+F10** (Run).

## Usage
1.  **Sign Up:** Create a new account to initialize your profile using email authentication or Google Sign In.
2.  **Add Tasks:** Tap the `+` button on the dashboard to create a new task. Assign it a category (e.g., "Work") and schedule dates.
3.  **Track:** Mark tasks as "Completed" by checking the box. Watch your progress bar fill up!
4.  **Analyze:** Visit the "Insights" tab to see your weekly productivity graph and productivity insights.

## License
This project is developed for learning purposes.  
All rights reserved by the developer.

## Project Status
Development is currently stopped.  
The core functionality is complete and stable. Further improvements and feature additions may be considered in the future.


## Contact
Created by **Monit Bisht** - Aspiring Android Developer.

* [GitHub Profile](https://github.com/monitbisht)
* [Email](monitbisht15@gmail.com)


## Acknowledgements
* **Android Developer Documentation**
* **Firebase Authentication and Firestore Documentation**
* **MPAndroidChart**
* **Material Design** 
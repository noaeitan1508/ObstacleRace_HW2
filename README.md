 Obstacle Race - 2D Android Game

 About The Project
Obstacle Race is a dynamic 2D mobile racing game built for Android using Kotlin. The player navigates a car through a wide 5-lane grid, dodging obstacles and collecting coins to achieve the highest distance score, complete with crash sound effects and an active odometer.

## Key Features & Implementations
* **Dual Control Modes & Dynamic Speed:** Players can steer using on-screen buttons (featuring both Slow and Fast modes) or switch to "Sensor Mode". In Sensor Mode, the device's accelerometer steers left/right via lateral tilting, while the bonus feature allows tilting forward/backward to dynamically adjust the speed of incoming obstacles.
* **Matrix-Based Movement:** The game logic avoids dynamic X/Y coordinates. Instead, it uses a Matrix/Grid Visibility system, creating smooth lane transitions fully optimized for both LTR and RTL device languages.
* **Fragments & Interactive High Scores:** The High Scores screen is built using two distinct Fragments: a Top 10 score table and a Google Map. Clicking a specific score in the table dynamically updates the map view to pinpoint where that record was achieved.
* **Real-Time GPS Integration:** Uses Google's `FusedLocationProviderClient` to request permissions and fetch the player's exact geographic location upon game over.
* **Persistent Storage:** Utilizes Android's `SharedPreferences` for a lightweight, database-free solution to save player scores, coins, and coordinates locally.

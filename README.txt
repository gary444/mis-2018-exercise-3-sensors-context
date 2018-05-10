Detection thresholds


To determine the music mode (none, running or cycling), our app uses three movement indicators:
 - fft dominant frequency: the frequency bin with the highest response from the fft process
 - fft energy: the average energy per bin from the fft process
 - speed: from location services

We found that it was difficult to differentiate reliably between the tree action modes, due to:
 - variations within the modes: cycling may produce a range of speeds from 5-40kmh, and different energy and frequency depending whether the person is pedalling or not. The position of the device on the body also affects the FFT output - in a pocket it may shake more, giving noisy FFT output, while if it is held it will produce a cleaner frequency response at a particular frequency.
 - unreliable speed monitoring from gps - although speed was the best indicator of mode, the values received were not always accurate and steady

General approach:
 - we analysed the input streams and assigned likelihood points for each mode depending on the input values.

Outline of thresholds:
 - Cycling: speed 5-35kmh, fft frequency below 1.0Hz, medium energy
 - Running: speed 5-15kmh, fft frequency 0-2Hz, high energy
 - None: speed below 5kmh or above 35 kmh, low energy
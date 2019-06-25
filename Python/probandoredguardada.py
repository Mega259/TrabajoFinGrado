import tensorflow as tf
import numpy as np

model = tf.keras.models.load_model('redsaved.h5')

model.summary()

# Load TFLite model and allocate tensors.
interpreter = tf.lite.Interpreter(model_path="PossibleBF.tflite")
interpreter.allocate_tensors()

# Get input and output tensors.
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Test model on random input data.
input_shape = input_details[0]['shape']
# change the following line to feed into your own data.
input_data = np.array(np.random.random_sample(input_shape), dtype=np.float32)
print(input_shape)
print(input_data)

interpreter.set_tensor(input_details[0]['index'], input_data)

interpreter.invoke()
output_data = interpreter.get_tensor(output_details[0]['index'])
print(output_data)
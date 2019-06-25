from tensorflow.keras.models import load_model, save_model
m = load_model('redsaved.h5')
m.save('redsaved.h5')
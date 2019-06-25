from __future__ import absolute_import, division, print_function

# %% Importing all libraries
import math
#Importing CSV reader
import csv

#Importing the tensorflow package
import tensorflow as tf
from tensorflow import keras


#Helping libraries
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.preprocessing import normalize
import random



# %% Get the data
#ReadingData
class_names=['Reposo', 'Andando', 'Corriendo', 'Caida']

def reading_data(path):
    """ 
    Funtion that reads the data from a file path
    
    Args:
        path: The path of the document we want to read
    Returns:
        data: a list with 4 elements, X, Y, Z and R, the label of the movement
    """
    data = []
    with open(path, "r+") as csvdatafile:
        reader = csv.reader(csvdatafile, 'excel')
        for row in reader:
            data.append((float(row[0]),float(row[1]),float(row[2]),int(row[3])))
    return data

def random_split(list_to_separate, training_size):
    """ 
    Funtion that randomly splits the list into two list
    
    Args:
        list_to_separate: The list we intend to separate
        
        training_size: Integer
        
    Return:
        training: the list with size len(training_size)
        
        testing: the list with size [len(list_to_separate)-len(training_size)]
    """
    
    training = []
    test = []
    m=len(list_to_separate)
    which = ([training]*training_size) + ([test]*(m-training_size))
    random.shuffle(which)
    for array, sample in zip(which, list_to_separate):
        array.append(sample)
    return training, test
	
def tipifylist(list_to_tipify):
	"""
	Function that tipifies the list, every value minus the mean get divided by the standard deviation
	
	Args:
		list_to_tipify: The list we intend to tipify
		
	Return:
		list_to_tipify: The list already tipified
	
	"""

	number_of_data = len(list_to_tipify)
	media, desv = [], []
	
	x_media = 0
	y_media = 0
	z_media = 0
	for row in list_to_tipify:
		x_media = x_media + row[0]
		y_media = y_media + row[1]
		z_media = z_media + row[2]
		
	media = [x_media/number_of_data, y_media/number_of_data, z_media/number_of_data]
	print(media)
	x_desv = 0
	y_desv = 0
	z_desv = 0
	for row in list_to_tipify:
		x_desv = (row[0]-media[0])*(row[0]-media[0]) + x_desv
		y_desv = (row[1]-media[1])*(row[1]-media[1]) + y_desv
		z_desv = (row[2]-media[2])*(row[2]-media[2]) + z_desv
		
	desv = [math.sqrt(x_desv/(number_of_data-1)), math.sqrt(y_desv/(number_of_data-1)), math.sqrt(z_desv/(number_of_data-1))]
	print(desv)

	for row in list_to_tipify:
		row = [(row[0]-media[0])/desv[0], (row[1]-media[1])/desv[1], (row[2]-media[2])/desv[2]]
		
	return list_to_tipify
	
def positive_list(list_to_positivice):
	"""
	Function that make the 3 first columns of a list positive
	
	Args:
		list_to_positivice: The list we intend to get positive values on
		
	Return:
		list_to_positivice: The list already positive
	"""
	maxX=0
	maxY=0
	maxZ=0
	for row in list_to_positivice:
		if(abs(row[0])>maxX):
			maxX=abs(row[0])
		if(abs(row[1])>maxY):
			maxY=abs(row[1])
		if(abs(row[2])>maxZ):
			maxZ=abs(row[2])	

	for row in list_to_positivice:
		row=[row[0]+maxX, row[1]+maxY, row[2]+maxZ, row[3]]	
		
	return list_to_positivice
		

#Preparing the data to be separated into testing and training datasets
alltraining=[]
alltesting=[]
alldata= []

alldata = reading_data('completo+respuesta.csv')

#alldata = positive_list(alldata)

alldata = tipifylist(alldata)

alltraining, alltesting = random_split(alldata, int(0.85*len(alldata)))


# %% Preprocessing the data

training_var, training_res, testing_var, testing_res = [],[],[],[]
alldata_res, alldata_var = [], []

#Separating the data into the accelerometer vector and the result
for row in alltraining:
	training_var.append((float(row[0]),float(row[1]),float(row[2])))
	training_res.append(int(row[3]))

training_res=keras.utils.to_categorical(np.array(training_res))
training_var=np.array(training_var)
#training_var=normalize(training_var, axis=0, norm='max')
	
for row in alltesting:
	testing_var.append((float(row[0]),float(row[1]),float(row[2])))
	testing_res.append(int(row[3]))

testing_res=keras.utils.to_categorical(np.array(testing_res))
testing_var=np.array(testing_var)
#testing_var=normalize(testing_var, axis=0, norm='max')


# %% Definition of NN

#Definition of the NN
model = keras.Sequential([
		keras.layers.Dense(1, input_dim=3),
		keras.layers.Dense(50 , activation=tf.nn.relu6),
		keras.layers.Dense(150 , activation=tf.nn.sigmoid),
		keras.layers.Dense(250 , activation=tf.nn.sigmoid),
		keras.layers.Dense(150 , activation=tf.nn.sigmoid),
        keras.layers.Dense(50 , activation=tf.nn.relu6),
		keras.layers.Dense(4, activation=tf.nn.softmax)
		])

# %% Compilation of the NN and final results

#Compilation of NN
model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

weights = [0.8, 1.0, 1.0, 1.0]
epoch=71
hist=model.fit(training_var, training_res, epochs=epoch, batch_size=8, class_weight=weights, validation_data=(testing_var, testing_res))

#test_loss, test_acc = model.evaluate(testing_var, testing_res)

#Some graphics statistics
train_loss=hist.history['loss']
val_loss=hist.history['val_loss']
train_acc=hist.history['acc']
val_acc=hist.history['val_acc']
xc=range(epoch)

plt.figure(1,figsize=(7,5))
plt.plot(xc,train_loss)	
plt.plot(xc,val_loss)
plt.xlabel('num of Epochs')
plt.ylabel('loss')
plt.title('train_loss vs val_loss')
plt.grid(True)
plt.legend(['train','val'])
plt.style.use(['classic'])

plt.figure(2,figsize=(7,5))
plt.plot(xc,train_acc)
plt.plot(xc,val_acc)
plt.xlabel('num of Epochs')
plt.ylabel('accuracy')
plt.title('train_acc vs val_acc')
plt.grid(True)
plt.legend(['train','val'],loc=4)
plt.style.use(['classic'])

#Matriz confusion todo
y_pred = model.predict_classes(training_var)
p=model.predict_proba(training_var)
print(classification_report(np.argmax(training_res,axis=1), y_pred,target_names=class_names))
print(confusion_matrix(np.argmax(training_res,axis=1), y_pred))

#Matriz de confusion test
y_pred = model.predict_classes(testing_var)
p=model.predict_proba(testing_var) # to predict probability
print(classification_report(np.argmax(testing_res,axis=1), y_pred,target_names=class_names))	
print(confusion_matrix(np.argmax(testing_res,axis=1), y_pred))

plt.show()

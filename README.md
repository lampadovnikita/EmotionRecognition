# EmotionRecognition
This repository represents an android application performing recognition of facial emotions on an image.

## The convolutional neural network used
To classify facial emotions application uses trained deep convolutional neural network (simple_classifier.tflite).  
This neural network has the following structure:  
<img src="/images/dnn_structure.png"  width="250" height="468">  

The DNN model trained on hybrid dataset. The dataset was split into two subsets: a train subset (80%) and a test subset (20%).  
Normalized confusion matrix:  
<img src="/images/normalized_confusion_matrix.png"  width="400" height="360">  

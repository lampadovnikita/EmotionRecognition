# EmotionRecognition
This repository represents an android application performing recognition of facial emotions on an image.  

## The hybrid dataset
To train the CNN model there used hybrid dataset composed of the following datasets images:
- CK+ (all images except contempt images)  
- JAFFE (all images)  
- FER2103 (all images)  
- RAF-DB (all images but only 205 happy class images)  

The resulting hybrid dataset has the following data distribution:  
<img src="/images/data_distribution.png"  width="350" height="238">  


## The convolutional neural network used
To classify facial emotions application uses trained deep convolutional neural network (simple_classifier.tflite).  
This neural network has the following structure:  
<img src="/images/dnn_structure.png"  width="250" height="468">  

The DNN model trained on hybrid dataset. The dataset was split into two subsets: a train subset (80%) and a test subset (20%).  
Normalized confusion matrix:  
<img src="/images/normalized_confusion_matrix.png"  width="400" height="360">  

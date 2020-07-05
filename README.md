# EmotionRecognition
This repository represents an android application performing recognition of facial emotions on an image.  

## Application


## The hybrid dataset
To train the CNN model there used hybrid dataset composed of the following datasets images:
- [CK+](https://www.researchgate.net/publication/224165246_The_Extended_Cohn-Kanade_Dataset_CK_A_complete_dataset_for_action_unit_and_emotion-specified_expression) (all images except contempt images).  
- [JAFFE](https://zenodo.org/record/3451524#.XuHa20UzZPY) (all images).  
- [FER2103](https://www.kaggle.com/deadskull7/fer2013) (all images).  
- [RAF-DB](http://whdeng.cn/RAF/model1.html) (all images but only 205 happy class images).  

The resulting hybrid dataset has the following data distribution:  
<img src="/images/data_distribution.png"  width="350" height="238">  
All images was converted into the FER2013 images format - greyscale 48*48 pixels.  

## The convolutional neural network used
To classify facial emotions application uses trained deep convolutional neural network (simple_classifier.tflite). The 
This neural network has the following structure:  
<img src="/images/dnn_structure.png"  width="250" height="468">  

The DNN model trained on hybrid dataset. The dataset was split into two subsets: a train subset (80%) and a test subset (20%).  
Normalized confusion matrix:  
<img src="/images/normalized_confusion_matrix.png"  width="400" height="360">  

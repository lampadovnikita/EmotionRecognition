# EmotionRecognition
This repository represents an android application performing recognition of facial emotions on an image.  

## The application
To detect faces on an image the application uses [ML Kit](https://developers.google.com/ml-kit).
After detection complete the face image area converted into greyscale 48*48 pixel format, each pixel represents as [0, 1] float number.
Finally, converted area fed to the [TensorFlow Light](https://www.tensorflow.org/lite/guide) convolutional neural network model (simple_classifier.tflite).
The model provide output that consist of probabilities for each class: angry, disgust, fear, happy, neutral, sad, surprise.  
<img src="/images/example.png" width="288" height="512">

## The hybrid dataset
To train the CNN model there used hybrid dataset composed of the following datasets images:
- [CK+](https://www.researchgate.net/publication/224165246_The_Extended_Cohn-Kanade_Dataset_CK_A_complete_dataset_for_action_unit_and_emotion-specified_expression) (all images except contempt images).  
- [JAFFE](https://zenodo.org/record/3451524#.XuHa20UzZPY) (all images).  
- [FER2013](https://www.kaggle.com/deadskull7/fer2013) (all images).  
- [RAF-DB](http://whdeng.cn/RAF/model1.html) (all images but only 205 happy class images).  

The resulting hybrid dataset contains 46614 images and has the following data distribution:  
<img src="/images/data_distribution.png"  width="350" height="238">  
All images was converted into the FER2013 images format - greyscale 48*48 pixels.  

## The convolutional neural network used
To classify facial emotions the application uses trained deep convolutional neural network (simple_classifier.tflite).
Each pixel converted from [0, 255] integer number to [0, 1] float number.
The neural network has the following structure:  
<img src="/images/dnn_structure.png"  width="250" height="468">  

| Parameter | Value |
| --- | --- |
|  min_delata | 0.0001 |
| patience | 10 |
| optimizer | Adam |
| learning_rate | 0.0001 |
| loss | categorical_crossentropy |
| batch_size | 96 |

The DNN model trained on hybrid dataset. The dataset was split into two subsets: a train subset (80%) and a test subset (20%).  
Normalized confusion matrix:  
<img src="/images/normalized_confusion_matrix.png"  width="400" height="360">  

| Metric | Value (Test subset) |
| --- | --- |
| Accuracy | 0.678 |
| Precision | 0.662 |
| F1 | 0.647 |

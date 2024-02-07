# HandwritingRecAndroid               
                          Off line handwritten text recognition using DL4J and OpenCV libs
                          
Off line подразумевает, что слово сегментируется на буквы, эти буквы проходят через нейронку и получается криво предсказанное слово.
On line - то как работает рукописный ввод. Анализ векторизованных каракуль человека. То есть там, наверное, используется рекуррентная 
нейронная сетьможет быть обычный алгоритм k-ближайших соседей. В этом случае нужна база ВСЕХ слов. 
                          
  1) Первая Activity - меню (StartButton.java), просто запускает камеру Activity (WordRecActivity)
  
  2) WordRecActivity. Наверное, я переименую этот класс "CameraActivity". Показывает на экране вид с задней камеры обводя 
прямоугольниками объекты похожие на слова, должен слова....

  3) SelectActivity - основной класс распознавания.
  
    SelectActivity запускает SelectScreen где можно выбрать именно те слова, которые надо распознать, перевести в печатный. 
После того как выбрали и нажали на кнопочку "перевести в текст" в этом же классе обрабатываются
эти слова. 

    Обработка выбранных слов: сортируем слова слева направо (можно добавить и по высоте);
находим буквы в слове (просто проводим контуры слова через morphologyEx и выбираем непрерывные контуры);
дальше копируем эти прямоугольные области(буквы) и передаем нейронке эти картинки в том виде на котором она обучалась, в этом случае 
на черно-белых, то есть значения равны либо 1-му, либо 0-ю; 
Дальше соединяем эти предсказанные буквы слева направо и получаем слово.

Слова передаются в RecognizedScreen где можно делать с ними все что угодно. Так же я добавил казахский словарь слов из которого 
находятся наиболее похожие слова.

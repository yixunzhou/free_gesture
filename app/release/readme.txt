使用前，
需在garthered_data2/目录下新建settings.txt文件，存放实验轮数等设置，换行分隔。
收集完一次最好重启软件再收集下一次。
settings.txt示例：
Devive_num:02
Single_task_time(seconds):1.5
Task_rounds:25
Action_times:1
Expriment_rounds:30
File_limit:0.9

说明：
格式 参数名:参数
第一行存放设备id
第二行存放每个任务的时长，单位为秒
第三行存放每次实验的任务轮数
第四行存放每次任务需要做的动作次数
第五行存放每个人需要做的实验次数
第六行存放阈值的百分比，阈值计算公式为：每次实验的任务轮数*每轮任务的任务个数*每次任务的动作次数*阈值百分比。截至目前该值为25*6*3*0.9。
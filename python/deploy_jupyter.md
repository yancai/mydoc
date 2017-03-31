# 简介
本文简介如何部署[jupyter](https://jupyter.org/)  
jupyter类似web版的ipython，吊炸天，帅爆了，很适合写代码示例或者技术文档


# 部署步骤

默认目录为：`/home/jupyter`

 1. 使用virtualenv创建虚拟环境  
    `virtualenv jupyter_env`
 2. 进入虚拟环境  
    linux：`source env/bin/active`
    windows：`jupyter_env\Scripts\activate`
 3. 安装jupyter  
    `pip install jupyter`
 4. 启动jupyter  
    `jupyter notebook --ip 0.0.0.0 --port 8000`  
    浏览器打开地址访问jupyter：http://ip:port

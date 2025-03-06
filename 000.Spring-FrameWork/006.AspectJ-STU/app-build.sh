#!/bin/bash
# 1. 先安装aspectj: java -jar aspectj-1.9.22.1.jar

# 2. 使用ajc编译, 将源代码文件转为.class文件
/Users/wang/WorkSpace/apps/AspectJ/bin/ajc -d bin -classpath /Users/wang/WorkSpace/apps/AspectJ/lib/aspectjrt.jar App.java  AjAspect.aj 
# 此时，对应的class文件在bin目录下，反编译一下，就可以知道织入方式了

# 3. 执行
cd bin
java -classpath /Users/wang/WorkSpace/apps/AspectJ/lib/aspectjrt.jar:. App
# 单一主键的好处 3NF

# 为啥主键是Long，而不是String
>   Long 
> String 优点：存储信息量大，扩展性强。 
> Long   优点：存储空间小,64bit
> String 缺点：存储所需空间大。(uuid 32字节)
> Long   缺点：存储只需4字节表示范围有限
>String VS Long 结论
两者相比,均无压倒对方优势，本框架鉴于高度引用的主导思想，使用Long(Snowflake)

##为啥不是自增长主键
> 添加数据时，ID由数据库生成，多1次查询。

##引用时使用外键吗？
> 优点：使用外键，自带索引，数据的完整性，一致性都能满足。
> 缺点：依赖关系复杂，添加数据必须是正确的。为单体测试带来困难。
> 结论：单体测试时，全部不使用外键，正常业务数据时，使用外键

##distinct有存在价值吗？
>group by总是比distinct效率高
>count时，disctinct可以和count结合，而groupby必须使用子查询,但是groupby速度快

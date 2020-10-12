# spring-cache-redis-plus
spring cache 扩展

#
支持redis缓存注解支持缓存失效时间和临期自动更新缓存

#使用方式
"cache:subject:recommend#失效时间#临期重新生成时间"

@Cacheable(value = "cache:subject:recommend#300#60", key = "")
<template>
    <el-menu :default-active="this.$router.path" router>
      <el-menu-item v-for="(item,i) in navList" :key="i" :index="item.name">
        {{ item.navItem }}
      </el-menu-item>
    </el-menu>
</template>

<script>
    export default {
        name: "Navbar",
        data() {
            return {
                activeIndex: '1',
                activeIndex2: '1',
                navList:[
                    {name:'/client',navItem:'客户管理'},
                    {name:'/server',navItem:'邮件管理'},
                    {name:'/system',navItem:'系统参数设置'},
                    {name:'/ip',navItem:'ip过滤控制'},
                    {name:'/password',navItem:'管理员修改密码'},
                    {name:'/log',navItem:'日志管理'},
                    {name:'/send',navItem:'群发邮件'}
                ]
            };
        },
        methods: {
            handleSelect(key, keyPath) {
                console.log(key, keyPath);
            }
        }
    }
</script>

<style scoped>

</style>

<template>
  <el-container>
    <el-header>
      <Header/>
    </el-header>
    <el-container>
      <el-aside>
        <Navbar/>
      </el-aside>
      <el-main>
        <el-card>
          <el-form ref="form" :model="form" label-width="80px">
            <el-form-item label="发件账号">
              <el-select v-model="form.from" placeholder="请选择">
                <el-option
                  v-for="item in options"
                  :key="item.mailAccount"
                  :value="item.mailAccount">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="收件人">
              <el-input v-model="form.to" placeholder="输入多个用户用分号分隔，末尾请不要输入分号"></el-input>
            </el-form-item>
            <el-form-item label="主题">
              <el-input v-model="form.subject"></el-input>
            </el-form-item>
            <el-form-item label="内容">
              <el-input type="textarea" class="mail-content" :rows="25" v-model="form.content"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onSubmit">发送</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
    import Header from "../components/Header";
    import Navbar from "../components/Navbar";
    import Global from "../components/Global";
    export default {
        name: "Send",
        components: {Navbar, Header},
        beforeRouteEnter: (from, to, next) => {
            next(vm => {
                vm.getAccount();
            });
        },
        data(){
            return{
                form:{
                    from:'',
                    to:'',
                    subject:'',
                    content:''
                },
                options:[{
                    mailAccount:'',
                }]
            }
        },
        methods:{
            onSubmit(){
                this.axios({
                    method: 'post',
                    url: Global.httpUrl + 'admin/sendMail',
                    data: JSON.stringify({
                        from: this.form.from,
                        to: this.form.to,
                        subject: this.form.subject,
                        content: this.form.content,
                    }),
                    headers: {
                        'Content-Type': 'application/json',
                        'token':Global.token
                    }
                }).then(response => {
                    if (response.data.code === 200) {
                        this.$notify({
                            title: '成功',
                            message: '发送邮件成功',
                            type: 'success'
                        });
                        this.initData();
                    } else {
                        this.$notify.error({
                            title: '失败',
                            message: response.data.message,
                        });
                    }
                })
            },
            getAccount(){
                this.axios({
                    method: 'get',
                    url: Global.httpUrl + 'user/info/accounts',
                    headers: {
                        'Content-Type': 'application/json',
                        'token': Global.token
                    }
                }).then(response => {
                    console.log(response);
                    this.options = response.data.data;
                }).catch(error => {
                    console.log(error);
                })
            }

        }
    }
</script>

<style scoped>
.mail-content{
}
</style>

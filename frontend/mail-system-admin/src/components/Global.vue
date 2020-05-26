<template>

</template>

<script>
    const httpUrl = 'http://192.168.43.1:8080/api/v1/';
    const token = '';
    function setToken(token) {
        this.token = token;
    }
    export default {
        name: "Global",
        httpUrl,
        setToken,
        token
    }
</script>

<style scoped>

</style>

const token = searchParam('token')
if (token) {
    localStorage.setItem("token", token)
}

function searchParam(key) {
    return new URLSearchParams(location.search).get(key);
}

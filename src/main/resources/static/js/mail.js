// To Email
const emailAuthButton = document.getElementById('email-btn');

if (emailAuthButton) {
    emailAuthButton.addEventListener('click', async () => {
        const email = document.getElementById('email').value.trim();

        try {
            const params = new URLSearchParams({ email });
            const res = await fetch(`/api/mail?${params.toString()}`, {
                method: 'POST'
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(`서버 오류 ${res.status}\n${text}`);
            }
            alert(await res.text());

        } catch (err) {
            console.error(err);
            alert("제대로 된 이메일을 입력해주세요.");
        }
    });
}

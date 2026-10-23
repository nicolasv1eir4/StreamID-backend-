document.addEventListener('DOMContentLoaded', () => {

    // Função responsável por alternar a visualização das telas
    function navegarPara(idTelaDestino) {
        // Oculta todas as telas removendo a classe 'active'
        const telas = document.querySelectorAll('.screen');
        telas.forEach(tela => {
            tela.classList.remove('active');
        });

        // Torna visível apenas a tela desejada
        const telaDestino = document.getElementById(idTelaDestino);
        if (telaDestino) {
            telaDestino.classList.add('active');
        }
    }

    // 1. Tela Inicial -> Tela Delay (ao clicar no botão Iniciar)
    const btnIniciar = document.getElementById('btn-iniciar');
    if (btnIniciar) {
        btnIniciar.addEventListener('click', () => {
            navegarPara('screen-delay');

            // Aguarda 2 segundos na tela de carregamento (Delay) e vai para o Login
            setTimeout(() => {
                navegarPara('screen-login');
            }, 2000);
        });
    }

    // 2. Tela Login -> Tela Cadastro (ao clicar no botão pequeno 'Cadastrar-se')
    const btnGoCadastro = document.getElementById('btn-go-cadastro');
    if (btnGoCadastro) {
        btnGoCadastro.addEventListener('click', () => {
            navegarPara('screen-cadastro');
        });
    }

    // Função auxiliar para chamar a API
    async function chamarApi(caminho, dados) {
        const resposta = await fetch(caminho, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dados)
        });
        const corpo = await resposta.json();
        if (!resposta.ok) throw new Error(corpo.erro || 'Erro inesperado.');
        return corpo;
    }

    // 3. Login
    const formLogin = document.getElementById('form-login');
    if (formLogin) {
        formLogin.addEventListener('submit', async (evento) => {
            evento.preventDefault();
            const email = formLogin.querySelector('input[type="email"]').value;
            const senha = formLogin.querySelector('input[type="password"]').value;

            try {
                await chamarApi('api/login', { email, senha });
                navegarPara('screen-home');
                formLogin.reset();
            } catch (erro) {
                alert(erro.message);
            }
        });
    }

    // 4. Cadastro
    const formCadastro = document.getElementById('form-cadastro');
    if (formCadastro) {
        formCadastro.addEventListener('submit', async (evento) => {
            evento.preventDefault();
            const campos = formCadastro.querySelectorAll('input');
            const nome = campos[0].value;
            const email = campos[1].value;
            const senha = campos[2].value;
            const confirma = campos[3].value;

            if (senha !== confirma) {
                alert('As senhas não coincidem.');
                return;
            }

            try {
                await chamarApi('api/cadastro', { nome, email, senha });
                alert('Conta criada com sucesso! Por favor, faça login para continuar.');
                navegarPara('screen-login');
                formCadastro.reset();
            } catch (erro) {
                alert(erro.message);
            }
        });
    }

    // 5. Link de Retorno: Cadastro -> Login
    const linkVoltarLogin = document.getElementById('link-voltar-login');
    if (linkVoltarLogin) {
        linkVoltarLogin.addEventListener('click', (evento) => {
            evento.preventDefault();
            navegarPara('screen-login');
        });
    }

});
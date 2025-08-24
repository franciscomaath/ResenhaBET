# **🚀 ResenhaBET: A Plataforma de Apostas da Resenha\! 🚀**

Bem-vindo ao ResenhaBET, o sistema de apostas em tempo real que transforma os nossos campeonatos de EA FC numa experiência lendária\! ⚽️💸

Este projeto funciona em conjunto com o [**ResenhaScore365**](https://github.com/franciscomaath/ResenhaScore365), utilizando os dados de "Aura" (habilidade) de cada jogador para gerar odds realistas e permitir que a galera aposte em tempo real nas partidas. Chega de "achismos", agora a zoeira é baseada em dados\!

## **✨ Funcionalidades Principais**

* **Odds Dinâmicas:** As probabilidades são calculadas com base na Aura (Elo) dos jogadores, trazendo o realismo do desporto para a nossa competição.  
* **Apostas em Tempo Real:** Uma interface web simples onde todos podem fazer as suas apostas e ver as atualizações instantaneamente.  
* **Painel de Administrador Completo:** Controlo total sobre o campeonato na ponta dos seus dedos\!  
  * ✅ Criar e resolver partidas.  
  * ✅ Abrir e fechar o mercado de apostas com um clique.  
  * ✅ Atualizar placares em tempo real.  
  * ✅ Importar Auras do ResenhaScore365 **sem reiniciar o servidor**.  
  * ✅ Gerir carteiras e adicionar convidados surpresa\!  
* **Segurança dos Dados:** Funcionalidades de **Importar/Exportar** para salvar e carregar o estado das carteiras e do histórico, garantindo que nenhuma aposta seja perdida.

## **🛠️ Guia de Instalação e Configuração (Ubuntu)**

Vamos colocar essa máquina para rodar\!

### **1\. Pré-requisitos 🐧**

* **Instale o Node.js e o NPM** (se ainda não tiver):  
  sudo apt update  
  sudo apt install nodejs npm

### **2\. Estrutura de Pastas 📂**

O seu projeto precisa de estar organizado desta forma:

resenhabet/  
├── server.js           \# O cérebro do sistema 🧠  
├── package.json        \# Ficheiro de configuração  
├── \[ARQUIVO\_DE\_AURAS\].json  \# O seu ficheiro exportado do ResenhaScore365  
└── public/  
    └── index.html      \# A interface para a galera 🎮

### **3\. Instalação e Preparação ⚙️**

1. **Instale as Dependências:**  
   * Abra o terminal na pasta resenhabet/.  
   * Rode o comando mágico:  
     npm install

2. **Prepare o Ficheiro de Auras:**  
   * Pegue no ficheiro .json mais recente que você exportou do **ResenhaScore365**.  
   * Coloque-o na pasta resenhabet/.  
   * **MUITO IMPORTANTE:** Abra o server.js e verifique se o nome do seu ficheiro JSON corresponde ao que está na constante DATA\_FILE.

## **🚀 Como Usar a Plataforma**

### **1\. Ligar o Servidor 🟢**

* No terminal, dentro da pasta resenhabet/, execute:  
  node server.js

* O terminal vai mostrar o seu **endereço de IP local** (ex: http://192.168.1.10:3000). Guarde este link\!

### **2\. Aceder à Festa 🎉**

* **Para si (Admin):** No seu PC, abra o navegador e vá para http://localhost:3000.  
* **Para os seus amigos (na mesma rede Wi-Fi):** Envie o link com o seu IP (ex: http://192.168.1.10:3000). Eles entram, escolhem o nome e a brincadeira começa\!

### **3\. Guia do Administrador 👑**

O painel de administrador é o seu centro de comando.

* **Criar Partida:** Selecione os jogadores, defina se é "mata-mata" e crie a partida. As odds aparecem na hora\!  
* **Gerir Apostas:** Use os botões **"Abrir"** e **"Fechar Apostas"** para controlar o mercado. Acompanhe quem está a apostar em quem em tempo real.  
* **Resolver a Partida:** No final do jogo, clique no vencedor. O sistema faz o resto: calcula os ganhos, atualiza as carteiras e prepara o terreno para a próxima rodada de zoeira.  
* **Gestão de Auras e Dados:**  
  * **Importar Auras:** Atualizou o ResenhaScore365? Carregue o novo JSON aqui **sem derrubar o servidor**\!  
  * **Exportar/Importar Histórico:** Salve e carregue o estado das carteiras para garantir que os dados estão seguros.

Agora sim\! Com este guia, você tem tudo o que precisa para comandar o ResenhaBET como um verdadeiro mestre. Que comecem os jogos\!

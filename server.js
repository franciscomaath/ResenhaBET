// ResenhaBET v1.5 - Servidor
// Rode este arquivo com: node server.js

const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const path = require('path');
const os = require('os');
const fs = require('fs');

const app = express();
const server = http.createServer(app);
const io = socketIo(server);

const PORT = 3000;
const DATA_FILE = 'resenha_bet_backup.json'; // Arquivo de auras

// --- ESTADO DO SERVIDOR (EM MEMÓRIA) ---
let initialPlayers = {};
let guestBettors = [];
let bettors = {};
let currentMatch = null;
let currentBets = [];
let betHistory = [];

let timerState = {
    running: false,
    phase: 'Não Iniciado',
    startTime: 0,
    elapsed: 0,
    intervalId: null
};

function loadInitialData() {
    try {
        if (fs.existsSync(DATA_FILE)) {
            const rawData = fs.readFileSync(DATA_FILE);
            const jsonData = JSON.parse(rawData);
            
            initialPlayers = {};
            jsonData.players.forEach(player => {
                initialPlayers[player.name] = { aura: player.elo };
            });
            console.log("Dados de Aura carregados com sucesso do arquivo JSON.");
        } else {
            console.error(`AVISO: Arquivo de dados '${DATA_FILE}' não encontrado. Usando dados de exemplo.`);
            initialPlayers = { "Francisco": { aura: 1000 }, "Tadeu": { aura: 1000 } };
        }
    } catch (error) {
        console.error("ERRO ao carregar ou processar o arquivo de dados:", error);
        initialPlayers = { "Francisco": { aura: 1000 }, "Tadeu": { aura: 1000 } };
    }
}

function initializeBettors() {
    bettors = {};
    const allPossibleBettors = [...Object.keys(initialPlayers), ...guestBettors];
    allPossibleBettors.forEach(name => {
        bettors[name] = { name: name, wallet: 50, hasBetted: false };
    });
    currentBets = [];
    betHistory = [];
}

function calculateOdds(p1Aura, p2Aura, isKnockout) {
    const probP1 = 1 / (1 + Math.pow(10, (p2Aura - p1Aura) / 400));
    const probP2 = 1 - probP1;

    if (isKnockout) {
        const oddP1 = Math.max(1.05, 1 / probP1).toFixed(2);
        const oddP2 = Math.max(1.05, 1 / probP2).toFixed(2);
        return { p1: oddP1, p2: oddP2, draw: null };
    } else {
        const drawProbability = 0.28;
        const winProbability = 1 - drawProbability;
        
        const adjustedProbP1 = probP1 * winProbability;
        const adjustedProbP2 = probP2 * winProbability;

        const oddP1 = Math.max(1.05, 1 / adjustedProbP1).toFixed(2);
        const oddP2 = Math.max(1.05, 1 / adjustedProbP2).toFixed(2);
        const oddDraw = Math.max(1.05, 1 / drawProbability).toFixed(2);
        
        return { p1: oddP1, p2: oddP2, draw: oddDraw };
    }
}

function getFullState() {
    const sanitizedTimerState = {
        running: timerState.running,
        phase: timerState.phase,
        startTime: timerState.startTime,
        elapsed: timerState.elapsed,
    };
    const allBettorNames = [...Object.keys(initialPlayers), ...guestBettors].sort();
    return { 
        bettors, 
        currentMatch, 
        players: allBettorNames, 
        currentBets, 
        betHistory, 
        timerState: sanitizedTimerState,
        auras: initialPlayers // Envia o objeto de auras para a nova aba
    };
}

app.use(express.static(path.join(__dirname, 'public')));
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

io.on('connection', (socket) => {
    console.log('Um novo apostador se conectou:', socket.id);

    socket.emit('initialState', getFullState());

    socket.on('setPlayer', (playerName) => {
        socket.playerName = playerName;
        console.log(`Socket ${socket.id} foi associado ao jogador ${playerName}`);
    });

    socket.on('placeBet', ({ bettorName, on, amount }) => {
        if (!currentMatch || !currentMatch.bettingOpen) {
            return socket.emit('betError', 'Apostas estão encerradas para esta partida.');
        }
        const bettor = bettors[bettorName];
        if (!bettor || bettor.wallet < amount) {
            return socket.emit('betError', 'Saldo insuficiente.');
        }
        if (amount <= 0) {
            return socket.emit('betError', 'O valor da aposta deve ser positivo.');
        }

        const existingBetIndex = currentBets.findIndex(bet => bet.bettor === bettorName);
        if (existingBetIndex > -1) {
            const oldBet = currentBets[existingBetIndex];
            bettor.wallet += oldBet.amount;
            currentBets.splice(existingBetIndex, 1);
        }
        
        currentBets.push({ bettor: bettorName, on: on, amount: parseFloat(amount) });
        bettor.wallet -= parseFloat(amount);
        bettor.hasBetted = true;
        
        console.log(`Aposta recebida de ${bettorName}: R$${amount} em ${on}`);
        io.emit('updateState', getFullState());
    });

    // --- Admin Events ---
    socket.on('admin:setMatch', ({ p1, p2, isKnockout }) => {
        // 1 - AURA PADRÃO GARANTIDA EM 1000
        const p1Aura = initialPlayers[p1]?.aura || 1000;
        const p2Aura = initialPlayers[p2]?.aura || 1000;
        
        currentMatch = {
            id: `match_${Date.now()}`,
            p1, p2, isKnockout,
            score1: 0, score2: 0, penaltyScore1: 0, penaltyScore2: 0,
            odds: calculateOdds(p1Aura, p2Aura, isKnockout),
            bettingOpen: false
        };
        currentBets = [];
        Object.values(bettors).forEach(b => b.hasBetted = false);
        
        console.log('Admin definiu nova partida:', currentMatch);
        io.emit('updateState', getFullState());
    });
    
    socket.on('admin:updateScore', ({ score1, score2, penaltyScore1, penaltyScore2 }) => {
        if (currentMatch) {
            currentMatch.score1 = score1;
            currentMatch.score2 = score2;
            currentMatch.penaltyScore1 = penaltyScore1;
            currentMatch.penaltyScore2 = penaltyScore2;
            io.emit('updateState', getFullState());
        }
    });

    socket.on('admin:toggleBetting', (isOpen) => {
        if (currentMatch) {
            currentMatch.bettingOpen = isOpen;
            console.log(`Admin ${isOpen ? 'abriu' : 'fechou'} as apostas.`);
            io.emit('updateState', getFullState());
        }
    });

    socket.on('admin:resolveMatch', ({ winner }) => {
        if (!currentMatch) return;

        const resolvedBets = [];
        currentBets.forEach(bet => {
            const bettor = bettors[bet.bettor];
            if (bet.on === winner) {
                const odd = winner === currentMatch.p1 ? currentMatch.odds.p1 : (winner === currentMatch.p2 ? currentMatch.odds.p2 : currentMatch.odds.draw);
                const payout = bet.amount * parseFloat(odd);
                bettor.wallet += payout;
                resolvedBets.push({ ...bet, outcome: 'win', payout });
            } else {
                resolvedBets.push({ ...bet, outcome: 'loss', payout: 0 });
            }
        });
        
        // 3 - GARANTINDO QUE O PLACAR SEJA SALVO NO HISTÓRICO
        const matchResult = { ...currentMatch };
        betHistory.unshift({ match: matchResult, bets: resolvedBets, winner });

        currentMatch = null;
        currentBets = [];
        io.emit('updateState', getFullState());
        console.log('Partida resolvida. Carteiras atualizadas.');
    });
    
    // 2 - NOVA FUNÇÃO PARA IMPORTAR AURAS SEM REINICIAR
    socket.on('admin:importAuras', (newAuraData) => {
        try {
            const jsonData = JSON.parse(newAuraData);
            if (!jsonData.players || !Array.isArray(jsonData.players)) {
                socket.emit('adminError', 'JSON inválido. A chave "players" não foi encontrada ou não é um array.');
                return;
            }
            
            const updatedAuras = {};
            jsonData.players.forEach(player => {
                if(player.name && typeof player.elo === 'number') {
                    updatedAuras[player.name] = { aura: player.elo };
                }
            });

            initialPlayers = updatedAuras;
            console.log("Auras atualizadas em tempo real via importação.");

            // Recalcula as odds da partida atual, se houver uma
            if (currentMatch) {
                const p1Aura = initialPlayers[currentMatch.p1]?.aura || 1000;
                const p2Aura = initialPlayers[currentMatch.p2]?.aura || 1000;
                currentMatch.odds = calculateOdds(p1Aura, p2Aura, currentMatch.isKnockout);
            }

            io.emit('updateState', getFullState());
            socket.emit('adminSuccess', 'Auras importadas e atualizadas com sucesso!');

        } catch (error) {
            console.error("Erro ao importar auras:", error);
            socket.emit('adminError', 'Erro ao processar o arquivo JSON.');
        }
    });

    socket.on('admin:resetWallets', () => {
        initializeBettors();
        io.emit('updateState', getFullState());
        console.log('Carteiras resetadas pelo admin.');
    });

    socket.on('admin:giveMoney', ({ playerName, amount }) => {
        if (bettors[playerName] && amount > 0) {
            bettors[playerName].wallet += parseFloat(amount);
            console.log(`Admin deu R$${amount} para ${playerName}.`);
            io.emit('updateState', getFullState());
        }
    });

    socket.on('admin:addGuestBettor', (guestName) => {
        const allBettorNames = [...Object.keys(initialPlayers), ...guestBettors];
        if (!allBettorNames.includes(guestName)) {
            guestBettors.push(guestName);
            bettors[guestName] = { name: guestName, wallet: 50, hasBetted: false };
            console.log(`Convidado ${guestName} adicionado.`);
            io.emit('updateState', getFullState());
        }
    });

    // Timer Controls
    socket.on('admin:setMatchPhase', (phase) => {
        if(timerState) timerState.phase = phase;
        io.emit('updateState', getFullState());
    });

    socket.on('admin:startTimer', () => {
        if (timerState && !timerState.running) {
            timerState.running = true;
            timerState.startTime = Date.now() - timerState.elapsed;
            timerState.intervalId = setInterval(() => {
                timerState.elapsed = Date.now() - timerState.startTime;
                io.emit('timerUpdate', { elapsed: timerState.elapsed });
            }, 1000);
            io.emit('updateState', getFullState());
        }
    });

    socket.on('admin:pauseTimer', () => {
        if (timerState && timerState.running) {
            timerState.running = false;
            clearInterval(timerState.intervalId);
            timerState.intervalId = null;
            io.emit('updateState', getFullState());
        }
    });

    socket.on('admin:resetTimer', () => {
        if(timerState) {
            timerState.running = false;
            clearInterval(timerState.intervalId);
            timerState.intervalId = null;
            timerState.elapsed = 0;
            timerState.phase = 'Não Iniciado';
            io.emit('updateState', getFullState());
        }
    });

    // Data Persistence
    socket.on('admin:exportData', (callback) => {
        const dataToExport = { bettors, betHistory, guestBettors };
        callback(dataToExport);
    });

    socket.on('admin:importData', (data) => {
        if (data && data.bettors && data.betHistory) {
            bettors = data.bettors;
            betHistory = data.betHistory;
            guestBettors = data.guestBettors || [];
            io.emit('updateState', getFullState());
            console.log("Dados de apostas importados com sucesso.");
        }
    });

    socket.on('disconnect', () => {
        console.log('Um apostador desconectou:', socket.id);
    });
});

server.listen(PORT, () => {
    loadInitialData();
    initializeBettors();
    console.log(`\n--- Servidor ResenhaBET v1.5 rodando na porta ${PORT} ---`);
    console.log("Para acessar na sua rede local, use um dos endereços abaixo:");
    
    const interfaces = os.networkInterfaces();
    Object.keys(interfaces).forEach(ifaceName => {
        interfaces[ifaceName].forEach(iface => {
            if (iface.family === 'IPv4' && !iface.internal) {
                console.log(`- http://${iface.address}:${PORT}`);
            }
        });
    });
    console.log(`- http://localhost:${PORT}`);
    console.log("\nAguardando os apostadores...");
});

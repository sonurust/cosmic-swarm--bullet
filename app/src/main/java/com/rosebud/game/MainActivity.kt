package com.rosebud.cosmicswarmbullet

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val webView: WebView = findViewById(R.id.webview)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        // Load the game HTML
        webView.loadDataWithBaseURL(null, getGameHtml(), "text/html", "utf-8", null)
    }
    
    private fun getGameHtml(): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <title>Cosmic Swarm</title>
    <style>
        body {
            margin: 0;
            overflow: hidden;
            background: black;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
        }
        canvas {
            border: 2px solid #333;
        }
        #hud {
            position: absolute;
            top: 10px;
            left: 10px;
            color: #fff;
            font-family: Arial, sans-serif;
            font-size: 20px;
        }
    </style>
</head>
<body>
    <div id="hud">Score: <span id="score">0</span><br>Time: <span id="time">0</span></div>
    <canvas id="gameCanvas" tabindex="0"></canvas>
    <script>
        const canvas = document.getElementById('gameCanvas');
        const ctx = canvas.getContext('2d');
        canvas.width = 800;
        canvas.height = 600;
        canvas.focus();

        const player = {
            x: canvas.width/2,
            y: canvas.height - 50,
            radius: 20,
            speed: 5,
            dx: 0,
            dy: 0,
            acceleration: 0.5,
            friction: 0.95,
            weapons: {
                fireRate: 150,
                damage: 1,
                spread: 1,
                lastFired: 0
            }
        };

        let particles = [];
        let projectiles = [];
        let enemies = [];
        let powerups = [];
        let score = 0;
        let gameTime = 0;
        let lastSpawnTime = 0;
        
        class Particle {
            constructor(x, y, color, speed, life) {
                this.x = x;
                this.y = y;
                this.color = color;
                this.speed = speed;
                this.life = life;
                this.maxLife = life;
                this.angle = Math.random() * Math.PI * 2;
            }

            update() {
                this.x += Math.cos(this.angle) * this.speed;
                this.y += Math.sin(this.angle) * this.speed;
                this.life--;
                return this.life > 0;
            }

            draw() {
                ctx.fillStyle = this.color;
                ctx.globalAlpha = this.life / this.maxLife;
                ctx.beginPath();
                ctx.arc(this.x, this.y, 2, 0, Math.PI * 2);
                ctx.fill();
                ctx.globalAlpha = 1;
            }
        }

        function createExplosion(x, y, color) {
            for(let i = 0; i < 20; i++) {
                particles.push(new Particle(x, y, color, Math.random() * 3 + 1, 50));
            }
        }

        function spawnEnemy() {
            const types = ['small', 'medium', 'large'];
            const type = types[Math.floor(Math.random() * types.length)];
            const enemy = {
                x: Math.random() * canvas.width,
                y: -20,
                type: type,
                radius: type === 'small' ? 10 : type === 'medium' ? 20 : 30,
                health: type === 'small' ? 1 : type === 'medium' ? 2 : 3,
                speed: type === 'small' ? 3 : type === 'medium' ? 2 : 1
            };
            enemies.push(enemy);
        }

        function spawnPowerup() {
            const types = ['fireRate', 'damage', 'spread'];
            powerups.push({
                x: Math.random() * canvas.width,
                y: -20,
                type: types[Math.floor(Math.random() * types.length)],
                radius: 15,
                speed: 2
            });
        }

        function autoFire() {
            const now = Date.now();
            if (now - player.weapons.lastFired > player.weapons.fireRate) {
                for(let i = 0; i < player.weapons.spread; i++) {
                    const angle = (i - (player.weapons.spread-1)/2) * 0.1;
                    projectiles.push({
                        x: player.x,
                        y: player.y,
                        radius: 3,
                        speed: 7,
                        angle: -Math.PI/2 + angle,
                        damage: player.weapons.damage
                    });
                }
                player.weapons.lastFired = now;
            }
        }

        function checkCollisions() {
            projectiles.forEach((projectile, pIndex) => {
                enemies.forEach((enemy, eIndex) => {
                    const dx = projectile.x - enemy.x;
                    const dy = projectile.y - enemy.y;
                    const distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < enemy.radius + projectile.radius) {
                        enemy.health -= projectile.damage;
                        projectiles.splice(pIndex, 1);
                        createExplosion(projectile.x, projectile.y, '#ff0');
                        if (enemy.health <= 0) {
                            enemies.splice(eIndex, 1);
                            score += 100;
                            createExplosion(enemy.x, enemy.y, '#f00');
                        }
                    }
                });
            });

            enemies.forEach(enemy => {
                const dx = player.x - enemy.x;
                const dy = player.y - enemy.y;
                const distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < player.radius + enemy.radius) {
                    gameOver();
                }
            });

            powerups.forEach((powerup, index) => {
                const dx = player.x - powerup.x;
                const dy = player.y - powerup.y;
                const distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < player.radius + powerup.radius) {
                    if (powerup.type === 'fireRate') player.weapons.fireRate = Math.max(50, player.weapons.fireRate - 20);
                    if (powerup.type === 'damage') player.weapons.damage += 0.5;
                    if (powerup.type === 'spread') player.weapons.spread = Math.min(5, player.weapons.spread + 1);
                    powerups.splice(index, 1);
                    createExplosion(powerup.x, powerup.y, '#0f0');
                }
            });
        }

        function gameOver() {
            alert(`Game Over! Score: ${score}`);
            location.reload();
        }

        function update() {
            // Player movement
            player.dx *= player.friction;
            player.dy *= player.friction;
            player.x += player.dx;
            player.y += player.dy;
            
            // Keep player in bounds
            player.x = Math.max(player.radius, Math.min(canvas.width - player.radius, player.x));
            player.y = Math.max(player.radius, Math.min(canvas.height - player.radius, player.y));

            // Update projectiles
            projectiles = projectiles.filter(projectile => {
                projectile.x += Math.cos(projectile.angle) * projectile.speed;
                projectile.y += Math.sin(projectile.angle) * projectile.speed;
                return projectile.y > 0;
            });

            // Update enemies
            enemies = enemies.filter(enemy => {
                enemy.y += enemy.speed;
                return enemy.y < canvas.height + enemy.radius;
            });

            // Update powerups
            powerups = powerups.filter(powerup => {
                powerup.y += powerup.speed;
                return powerup.y < canvas.height + powerup.radius;
            });

            // Update particles
            particles = particles.filter(particle => particle.update());

            // Spawn enemies and powerups
            const now = Date.now();
            if (now - lastSpawnTime > 1000) {
                spawnEnemy();
                if (Math.random() < 0.2) spawnPowerup();
                lastSpawnTime = now;
            }

            autoFire();
            checkCollisions();

            gameTime++;
            document.getElementById('score').textContent = score;
            document.getElementById('time').textContent = Math.floor(gameTime/60);
        }

        function draw() {
            ctx.fillStyle = '#000';
            ctx.fillRect(0, 0, canvas.width, canvas.height);

            // Draw player
            ctx.fillStyle = '#0ff';
            ctx.beginPath();
            ctx.arc(player.x, player.y, player.radius, 0, Math.PI * 2);
            ctx.fill();

            // Draw projectiles
            ctx.fillStyle = '#f0f';
            projectiles.forEach(projectile => {
                ctx.beginPath();
                ctx.arc(projectile.x, projectile.y, projectile.radius, 0, Math.PI * 2);
                ctx.fill();
            });

            // Draw enemies
            enemies.forEach(enemy => {
                ctx.fillStyle = enemy.type === 'small' ? '#f00' : enemy.type === 'medium' ? '#f60' : '#f90';
                ctx.beginPath();
                ctx.arc(enemy.x, enemy.y, enemy.radius, 0, Math.PI * 2);
                ctx.fill();
            });

            // Draw powerups
            powerups.forEach(powerup => {
                ctx.fillStyle = powerup.type === 'fireRate' ? '#0f0' : powerup.type === 'damage' ? '#ff0' : '#0ff';
                ctx.beginPath();
                ctx.arc(powerup.x, powerup.y, powerup.radius, 0, Math.PI * 2);
                ctx.fill();
            });

            // Draw particles
            particles.forEach(particle => particle.draw());
        }

        function gameLoop() {
            update();
            draw();
            requestAnimationFrame(gameLoop);
        }

        document.addEventListener('keydown', e => {
            const speed = player.acceleration;
            if (e.key === 'ArrowLeft' || e.key === 'a') player.dx -= speed;
            if (e.key === 'ArrowRight' || e.key === 'd') player.dx += speed;
            if (e.key === 'ArrowUp' || e.key === 'w') player.dy -= speed;
            if (e.key === 'ArrowDown' || e.key === 's') player.dy += speed;
        });

        // Touch controls
        let touchStartX = 0;
        let touchStartY = 0;
        canvas.addEventListener('touchstart', e => {
            e.preventDefault();
            touchStartX = e.touches[0].clientX;
            touchStartY = e.touches[0].clientY;
        });

        canvas.addEventListener('touchmove', e => {
            e.preventDefault();
            const touchX = e.touches[0].clientX;
            const touchY = e.touches[0].clientY;
            const dx = touchX - touchStartX;
            const dy = touchY - touchStartY;
            player.x += dx * 0.1;
            player.y += dy * 0.1;
            touchStartX = touchX;
            touchStartY = touchY;
        });

        gameLoop();
    </script>
</body>
</html>
        """.trimIndent()
    }
}
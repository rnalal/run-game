//gimmicks.js
//레벨별 기믹(웅덩이, 슬라이드, 몬스터, 밤 배경 등) 제어
export const gimmicks = {

    //현재 활성화된 기믹 상태
    active: {
        puddle: false,
        slideObstacle: false,
        monster: false,
        coinBurst: false,
        dark: false
    },

    //레벨 진입 시, 사용할 기믹 ON/OFF 설정
    activate(level) {
        //먼저 모드 끄고 레벨 조건에 맞게 다시 켬
        this.active = {
            puddle: false,
            slideObstacle: false,
            monster: false,
            coinBurst: false,
            dark: false
        };

        if (level >= 2) this.active.puddle = true;          //웅덩이
        if (level >= 4) this.active.slideObstacle = true;   //슬라이드 장애물
        if (level >= 6) this.active.monster = true;         //몬스터
        if (level >= 7) this.active.coinBurst = true;       //코인폭주
        if (level >= 8) this.active.dark = true;            //밤
    },

    /*
    onSpawn()
    - 스폰 타이밍마다 웅덩이,슬라이드,몬스터,코인폭주 추가
    - 이미 화면 근처에 있는 기믹과는 겹치지 않게 조정
    */
    onSpawn(level, obstacles, coins, playerX, groundY) {

        let spawnedPuddle = false;
        let spawnedSlide  = false;

        //화면 근처 장애물만 보고 슬라이드,웅덩이 중복 여부 체크
        const hasSlideAlready = obstacles.some(ob =>
            ob.type === "slideObstacle" &&
            ob.x > playerX - 300 &&
            ob.x < playerX + 900
        );

        const hasPuddleAlready = obstacles.some(ob =>
            ob.type === "puddle" &&
            ob.x > playerX - 300 &&
            ob.x < playerX + 900
        );

        //웅덩이 스폰(슬라이드가 없을 때 확률적으로)
        if (this.active.puddle && !hasSlideAlready && Math.random() < 0.28) {

            const spawnX = playerX + 420;

            const conflict = obstacles.some(ob =>
                ob.type !== "noTreeZone" &&
                Math.abs(ob.x - spawnX) < 160
            );

            if (!conflict) {
                //실제 충돌용 웅덩이
                obstacles.push({
                    type: "puddle",
                    x: spawnX,
                    y: groundY - 6,
                    w: 60,
                    h: 10,
                    collided: false
                });

                //주변 나무 스폰 막는 기상의 noTreeZone
                obstacles.push({
                    type: "noTreeZone",
                    x: spawnX - 80,
                    y: 0,
                    w: 240,
                    h: 200,
                    ghost: true
                });

                spawnedPuddle = true;
            }
        }

        //슬라이드 장애물(레벨4이상)
        if (this.active.slideObstacle && Math.random() < 0.22) {
            const spawnX = playerX + 450;

            const conflict = obstacles.some(ob =>
                Math.abs(ob.x - spawnX) < 160
            );

            if (!conflict) {
                //위에서 내려오는 가로바
                obstacles.push({
                    type: "slideObstacle",
                    x: spawnX,

                    y: groundY - 160,
                    targetY: groundY - 60,
                    dropSpeed: 3.2,

                    w: 110,
                    h: 24,
                    collided: false
                });

                //근처 다른 장애물 막기용 noTreeZone
                obstacles.push({
                    type: "noTreeZone",
                    x: spawnX - 120,
                    y: 0,
                    w: 300,
                    h: 220,
                    ghost: true
                });
                spawnedSlide = true;
            }
        }

        /*
        몬스터(레벨6이상)
        - 슬라이드 주변은 피해서 스폰
        */
        if (this.active.monster && Math.random() < 0.14) {

            const spawnX = playerX + 460;

            const nearSlide = obstacles.some(ob =>
                ob.type === "slideObstacle" &&
                Math.abs(ob.x - spawnX) < 220
            );

            if (!nearSlide) {
                obstacles.push({
                    type: "monster",
                    x: spawnX,
                    y: groundY - 38,
                    w: 34,
                    h: 32,
                    movePhase: Math.random() * 1000
                });
            }
        }

        //코인 폭주(레벨7,10구간)
        if (level === 7 || level === 10) {
            for (let i = 0; i < 20; i++) {
                coins.push({
                    x: playerX + 300 + i * 20,
                    y: groundY - 80 - Math.random() * 80,
                    r: 9,
                    taken: false
                });
            }
        }

        return {
            spawnedPuddle,
            spawnedSlide,
            spawnedTree: false
        };
    },

    /*
    onTick()
    - 특정 레벨에서 전체 이동 속도에 보정치 적용
    */
    onTick(level, speed) {
        if (level === 3) speed *= 1.12;
        if (level === 5) speed *= 1.12;
        if (level === 9) speed *= 1.18;
        return speed;
    },

    //몬스터 전용 이동 패턴 (좌우 흔들림)
    applyObstacleMovement(level, ob, t) {
        if (ob.type === "monster") {
            ob.x += Math.sin((t + ob.movePhase) / 200) * 2.0;
        }
    },

    /*
    onDraw()
    - 밤 레벨에서 화면 전체를 살짝 어둡게 덮기
    */
    onDraw(level, ctx, w, h, t) {
        if (this.active.dark) {
            ctx.fillStyle = "rgba(0,0,0,0.35)";
            ctx.fillRect(0, 0, w, h);
        }
    }
};

import random as r #add types
level=50
battype=1
class Move():
    def __init__(self,moveclass,dmg,typ,name):
        self.moveclass=moveclass
        self.dmg=dmg
        self.typ=typ
        self.name=name

class Pokemon:
    def __init__(self,hp,atk,Def,spa,spd,spe,typ1,typ2,moves,side,name,stab=1):
        self.hp=hp
        self.atk=atk
        self.Def=Def
        self.spa=spa
        self.spd=spd
        self.spe=spe
        self.typ1=typ1
        self.typ2=typ2
        self.moves=moves
        self.side=side
        self.name=name

        self.move=''
        self.movePower=0
        self.moveClass=''
        self.moveType=''
        self.random=r.randint(85,100)/100
        self.stab=1

Slash=Move('phys',70,'normal','Slash')
FuryCut=Move('phys',20,'bug','Fury Cutter')
AttOrd=Move('phys',90,'bug','Attack Order')
PowGem=Move('spe',80,'rock','Power Gem')
Discharge=Move('spe',80,'electric','Discharge')
HamArm=Move('phys',100,'fighting','Discharge')
Uproar=Move('spe',90,'normal','Uproar')
SkyDrop=Move('phys',60,'flying','Sky Drop')
NightSlash=Move('phys',70,'dark','Night Slash')
PsyCut=Move('phys',70,'psychic','Psycho Cut')
Peck=Move('phys',35,'flying','Peck')
MoongBeam=Move('spe',100,'ghost','Moongeist Beam')
PhantForce=Move('spe',80,'ghost','Phantom Force')
Conf=Move('spe',50,'psychic','Confusion')
NightDaze=Move('spe',85,'fighting','Night Daze')
StonEdge=Move('phys',100,'rock','Stone Edge')
RazShell=Move('phys',75,'water','Razor Shell')
Scratch=Move('phys',40,'normal','Discharge')
SunStrike=Move('phys',100,'steel','Sunsteel Strike')
ZenHb=Move('phys',80,'psychic','Zen Headbutt')
WildCharge=Move('phys',90,'electric','Wild Charge')
IronHead=Move('phys',80,'steel','Iron Head')

waspowerYou=Pokemon(70,80,102,80,102,40,'Bug','Flying',[Slash,FuryCut,AttOrd,PowGem],True,'Waspower')
waspowerOpp=Pokemon(70,80,102,80,102,40,'Bug','Flying',[Slash,FuryCut,AttOrd,PowGem],False,'Waspower')

thunderYou=Pokemon(79,115,70,80,125,80,'Electric','Flying',[Discharge,HamArm,Uproar,SkyDrop],True,'Thundercloudo')
thunderOpp=Pokemon(79,115,70,80,125,80,'Electric','Flying',[Discharge,HamArm,Uproar,SkyDrop],False,'Thundercloudo')

noctopusYou=Pokemon(86,92,88,68,75,73,'Dark','Psychic',[NightSlash,PsyCut,Peck,Slash],True,'Noctopus')
noctopusOpp=Pokemon(86,92,88,68,75,73,'Dark','Psychic',[NightSlash,PsyCut,Peck,Slash],False,'Noctopus')

blallgaeYou=Pokemon(43,29,131,29,131,37,'Psychic','Notype',[MoongBeam,PhantForce,Conf,NightDaze],True,'Blallgae')
blallgaeOpp=Pokemon(43,29,131,29,131,37,'Psychic','Notype',[MoongBeam,PhantForce,Conf,NightDaze],False,'Blallgae')

polyplypeYou=Pokemon(72,105,115,54,86,68,'Rock','Water',[StonEdge,Slash,RazShell,Scratch],True,'Polyplype')
polyplypeOpp=Pokemon(72,105,115,54,86,68,'Rock','Water',[StonEdge,Slash,RazShell,Scratch],False,'Polyplype')

infiniYou=Pokemon(137,137,107,113,89,97,'Psychic','Steel',[SunStrike,ZenHb,WildCharge,IronHead],True,'Infineline')
infiniOpp=Pokemon(137,137,107,113,89,97,'Psychic','Steel',[SunStrike,ZenHb,WildCharge,IronHead],False,'Infineline')

yourPokes=[waspowerYou,thunderYou,noctopusYou,blallgaeYou,polyplypeYou,infiniYou]
oppPokes=[waspowerOpp,thunderOpp,noctopusOpp,blallgaeOpp,polyplypeOpp,infiniOpp]
r.shuffle(yourPokes)
r.shuffle(oppPokes)
yours=yourPokes.pop(0)
opps=oppPokes.pop(0)
you=input('What is your name? ')
opp=input("What is your opponent's name?")
print()
print(f'Your pokemon is {yours.name}.')
print(f"The opponent's pokemon is {opps.name}.")

while yours.hp>=0 and opps.hp>=0:
    oppMove=r.choice(opps.moves)
    yourMove=r.choice(yours.moves)
    print(f'{yours.name} used {yourMove.name}!')
    print(f'{opps.name} used {oppMove.name}!')

    if yourMove.typ==yours.typ1 or yourMove.typ==yours.typ2:
        yours.stab=1.5
    if oppMove.typ==opps.typ1 or oppMove.typ==opps.typ2:
        opps.stab=1.5

    yours.random=r.randint(85,100)/100
    opps.random=r.randint(85,100)/100
    dmgToYouPhys=round(((2*level/5+2)*oppMove.dmg*opps.atk/yours.Def/50+2)*yours.random*opps.stab*battype)
    dmgToOppPhys=round(((2*level/5+2)*yourMove.dmg*yours.atk/opps.Def/50+2)*opps.random*yours.stab*battype)
    dmgToYouSpe=round(((2*level/5+2)*oppMove.dmg*opps.spa/yours.spd/50+2)*yours.random*opps.stab*battype)
    dmgToOppSpe=round(((2*level/5+2)*yourMove.dmg*yours.spa/opps.spd/50+2)*opps.random*yours.stab*battype)

    if yourMove.moveclass=='spe':
        opps.hp=opps.hp-dmgToOppSpe
    if yourMove.moveclass=='phys':
        opps.hp=opps.hp=dmgToOppPhys
    if oppMove.moveclass=='spe':
        yours.hp=yours.hp-dmgToYouSpe
    if oppMove.moveclass=='phys':
        yours.hp=yours.hp-dmgToYouPhys

    if yours.hp<=1:
        yours.hp=0
        print(f'{yours.name} has {yours.hp} health left!')
        print(f'{opps.name} has {opps.hp} health left!')
        print('The opponent wins!')
        break
    if opps.hp<=1:
        opps.hp=0
        print(f'{yours.name} has {yours.hp} health left!')
        print(f'{opps.name} has {opps.hp} health left!')
        print('You win!')
        break

    print(f'{yours.name} has {yours.hp} health left!')
    print(f'{opps.name} has {opps.hp} health left!')

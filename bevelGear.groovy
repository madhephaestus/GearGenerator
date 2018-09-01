List<Object>  makeGear(double numTeeth,double thickness,double bevelAngle,double toothBaseArchLen,double face, double helical){
	double toothAngle = (360.0)/numTeeth
	double baseThickness = toothBaseArchLen/Math.PI
	thickness-=baseThickness
	double baseDiam = (toothBaseArchLen*numTeeth)/Math.PI
	//double otherTeeth = (1.0/ratio)*numTeeth
	//double bevelAngle = Math.toDegrees(Math.atan2(otherTeeth,numTeeth))

	double topDiamOffset = thickness/(Math.tan(Math.toRadians(bevelAngle)))
	double topDiam = baseDiam-topDiamOffset*2
	
	//thickness-=baseThickness
	double baseDiamLower = baseDiam-(baseThickness*2* Math.cos(Math.toRadians(bevelAngle)))
	double totalThickness = thickness+baseThickness
	double toothDepth = baseThickness*1.5
	//println "Tooth Angle " +toothAngle+" tooth baseArchLen "+toothBaseArchLen+" base Diam "+ baseDiam+" top diam "+topDiam+" thickness "+thickness+" toothInset "+topDiamOffset
	//println " angle "+bevelAngle
	CSG upperSection =new Cylinder(baseDiam/2,topDiam/2,thickness,(int)numTeeth).toCSG() // a one line Cylinder
	CSG lowerSection =new Cylinder(baseDiamLower/2,baseDiam/2,baseThickness,(int)numTeeth).toCSG() // a one line Cylinder
					.toZMax()
	CSG blank = upperSection.union(lowerSection)
				.toZMin()
	if(bevelAngle<90)
		helical=0
	CSG toothCutter = new Cube(toothBaseArchLen,toothBaseArchLen,face+thickness*2).toCSG()
					.toXMin()
					.toYMin()
					.rotz(45)
					.scaley(0.4)
					.toZMax()
					.movez(thickness*1.5)
					.rotx(helical)
					.movex(-toothDepth)
					//.movez(thickness)
					.roty(90-bevelAngle)
					.movez(totalThickness)
					.movex(topDiam/2)
	double angleScale = 0.2
	double cutterOffset = toothAngle*angleScale
	toothCutter = toothCutter.rotz(-cutterOffset).union(toothCutter.rotz(cutterOffset)).hull()
	for(int i=0;i<numTeeth;i++){
		blank=blank.difference(toothCutter.rotz(toothAngle*i))
	}
	double pinRadius = ((3/16)*25.4)/2+0.1

     double pinLength = (2.5*25.4)
	CSG hole =new Cylinder(pinRadius,pinRadius,pinLength,(int)30).toCSG().movez(-pinLength/2) // steel reenforcmentPin
	return [blank.difference(hole)
	//.union(toothCutter)//.rotz(toothAngle*numTeeth/4)
	.rotz(180)
     ,baseDiam/2,toothAngle,toothDepth]
}

List<Object> makeBevelBox(Number numDriveTeeth, 
Number numDrivenTeeth,
Number thickness,
Number toothBaseArchLen, 
double axelAngle = 90,
double helical=0,
Number meshInterference = null){
	if(axelAngle>90)axelAngle=90
	if(axelAngle<0)axelAngle=0
	double baseThickness = toothBaseArchLen/Math.PI
	axelAngle=Math.toRadians(axelAngle)
	double bevelTriangleAngle = Math.PI-axelAngle
	// c² = b² + a² - 2ba cosC
	double lengthOfBevelCenter  =  Math.sqrt(
		Math.pow(numDriveTeeth,2)+
		Math.pow(numDrivenTeeth,2)-
		2.0*numDrivenTeeth*numDriveTeeth*Math.cos(bevelTriangleAngle)
		)
	//￼￼￼
	double Kvalue = numDrivenTeeth*numDriveTeeth*Math.sin(bevelTriangleAngle)/2.0
	double height = 2*Kvalue/lengthOfBevelCenter
	
	
	double bevelAngle =Math.acos(height/numDrivenTeeth)
	double bevelAngleB = Math.acos(height/numDriveTeeth)
	double face  = (thickness-baseThickness)/Math.sin(bevelAngle)
	double otherThick = face*Math.sin(bevelAngleB)+baseThickness
	
	//println "\n\nHeight "+(thickness-baseThickness)
	//println "Face "+face
	//println "Other Thickness "+otherThick
	def gearA = makeGear(numDriveTeeth,thickness,Math.toDegrees(bevelAngle),toothBaseArchLen,face,helical)
	def gearB = makeGear(numDrivenTeeth,otherThick,Math.toDegrees(bevelAngleB),toothBaseArchLen,face,-helical)
	if(meshInterference==null)
		meshInterference= new Double(gearA.get(3)*Math.cos(axelAngle)*0.75+0.1)
	double aDiam = gearB.get(1)*Math.cos(axelAngle)+
				gearA.get(1)-meshInterference// mesh interference distance
	double bDiam = gearB.get(1)*Math.sin(axelAngle)
	double bangle = gearB.get(2)
	//println bangle
	CSG shaft = new Cylinder(0.1,0.1,50,6).toCSG()
	CSG bevelShaft = shaft.roty(90-Math.toDegrees(bevelAngle))
					.movex(gearA.get(1))
	CSG bevelShaftB = shaft.roty(90-Math.toDegrees(bevelAngleB))
					.movex(gearB.get(1))
					.rotz(180)
	CSG gearBFinal = gearB.get(0)
					.rotz(bangle/2)
					//.union(shaft)
					//.union(bevelShaftB)
					.roty(Math.toDegrees(axelAngle))
					.movex(aDiam)
					.movez(bDiam)
					.rotz(180)
	CSG gearAFinal = gearA.get(0)//.rotz(bangle/4)//.union(shaft).union(bevelShaft)
	double ratio = (gearA.get(1)-meshInterference)/(gearB.get(1)-meshInterference)
	return [gearAFinal,gearBFinal,aDiam ,bDiam,Math.toDegrees(bevelAngle),face,otherThick, ratio,bangle,gearA.get(2)]
}
double computeGearPitch(double diameterAtCrown,double numberOfTeeth){
	return ((diameterAtCrown/2)*((360.0)/numberOfTeeth)*Math.PI/180)
}

if(args != null)
	return makeBevelBox(args)
double helical =20
// call a script from another library
def bevelGears = makeBevelBox([41,20,6,computeGearPitch(26.15,24),40,helical])

//Print parameters returned by the script
println "Bevel gear axil center to center " + bevelGears.get(2)
println "Bevel gear axil Height " + bevelGears.get(3)
println "Bevel angle " + bevelGears.get(4)
println "Bevel tooth face length " + bevelGears.get(5)
println "Gear B computed thickness " + bevelGears.get(6)
println "Gear Ratio " + bevelGears.get(7)
// return the CSG parts
return [	bevelGears,
		makeBevelBox([42,20,6,6,0,0]).collect{
			try{
				return it.movey(bevelGears.get(2)*2)
			}catch(Exception e){
				return it
			}
			},
		makeBevelBox([41,22,4,6,90,0]).collect{
			try{
				return it.movey(-bevelGears.get(2)*2)
			}catch(Exception e){
				return it
			}
			},
]
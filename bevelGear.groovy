//Your code here



List<Object>  makeGear(int numTeeth,def thickness){
	double toothAngle = (360.0)/24.0
	double toothBaseArchLen = (26.15/2)*toothAngle*Math.PI/180
	
	double baseDiam = (toothBaseArchLen*numTeeth)/Math.PI
	double topDiam = baseDiam-(thickness*2)
	double baseThickness = toothBaseArchLen/Math.PI
	double baseDiamLower = baseDiam-(baseThickness*2)
	double totalThickness = thickness+baseThickness
	double toothDepth = baseThickness*2
	println "Tooth Angle " +toothAngle+" tooth baseArchLen "+toothBaseArchLen+" base Diam "+ baseDiam+" top diam "+topDiam
	CSG upperSection =new Cylinder(baseDiam/2,topDiam/2,thickness,(int)numTeeth).toCSG() // a one line Cylinder
	CSG lowerSection =new Cylinder(baseDiamLower/2,baseDiam/2,baseThickness,(int)numTeeth).toCSG() // a one line Cylinder
					.toZMax()
	CSG blank = upperSection.union(lowerSection)
				.toZMin()
	CSG toothCutter = new Cube(toothBaseArchLen,toothBaseArchLen,baseDiam).toCSG()
					.toXMin()
					.toYMin()
					.rotz(45)
					.scaley(0.5)
					.toZMax()
					//.movez(thickness)
					.roty(45)
					.movez(totalThickness)
					.movex(topDiam/2-toothDepth)
	toothCutter = toothCutter.rotz(-toothAngle/8).union(toothCutter.rotz(toothAngle/8)).hull()
	for(int i=0;i<numTeeth;i++){
		blank=blank.difference(toothCutter.rotz(toothAngle*i))
	}
	
	return [blank,baseDiam/2,toothAngle]
}

List<Object> makeBevelBox(int numDriveTeeth, int numDrivenTeeth,Number thickness){
	def gearA = makeGear(numDriveTeeth,thickness)
	def gearB = makeGear(numDrivenTeeth,thickness)
	double aDiam = gearA.get(1)
	double bDiam = gearB.get(1)
	double bangle = gearB.get(2)
	CSG gearBFinal = gearB.get(0)
					.rotz(bangle/2)
					.rotx(-90)
					.movey(aDiam)
					.movez(bDiam)
	
	return [gearA.get(0),gearBFinal]
}


return makeBevelBox(24,24,4.45)
from mininet.topo import Topo

class MyTopo( Topo ):
	"MyTopo topology."

	def __init__( self ):

		Topo.__init__( self )

		# Initialize topology
		Topo.__init__( self )

		# Add hosts and switches 
		host1 = self.addHost( 'h1' )
		host2 = self.addHost( 'h2' )
		host3 = self.addHost( 'h3' )
		host4 = self.addHost( 'h4' )
		leftSwitch = self.addSwitch( 's1' )
		rightSwitch = self.addSwitch( 's2' )

		# Add links
		self.addLink( host1, leftSwitch )
		self.addLink( host2, leftSwitch )
		self.addLink( host3, rightSwitch )
		self.addLink( host4, rightSwitch )

topos = { 'mytopo': ( lambda: MyTopo() ) }

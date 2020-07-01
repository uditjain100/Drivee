package udit.programmer.co.drivee.Models

class Customer() {

    var firstName: String = ""
    var lastName: String = ""
    var number: String = ""

    constructor(
        firstName: String,
        lastName: String,
        number: String
    ) : this() {
        this.firstName = firstName
        this.lastName = lastName
        this.number = number
    }

}
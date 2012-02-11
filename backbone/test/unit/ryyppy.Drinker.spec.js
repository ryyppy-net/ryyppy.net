describe('Drinker module', function () {
    it('Construction', function () {
        var person = new Drinker();
        expect(person).not.toBeUndefined();
        expect(person.get('name')).toBe('Anonymous');
        expect(person.get('drinks')).toBe(0);
    });

    it('Having drinks', function () {
        var person = new Drinker();
        expect(person.get('drinks')).toBe(0);
        person.drink();
        expect(person.get('drinks')).toBe(1);
    });
});
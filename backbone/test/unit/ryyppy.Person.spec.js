describe('User module', function () {
    it('Person construction', function () {
        var person = new Person();
        expect(person).not.toBeUndefined();
        expect(person.get('name')).toBe('Anonymous');
        expect(person.get('drinks')).toBe(0);
    });
});